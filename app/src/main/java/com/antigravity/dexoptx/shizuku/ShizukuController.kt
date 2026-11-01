package com.antigravity.dexoptx.shizuku

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.antigravity.dexoptx.model.CompileStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Method

data class DexoptDetails(
    val packageName: String,
    val status: CompileStatus,
    val filter: String,
    val reason: String? = null,
    val primaryAbi: String? = null,
    val odexPath: String? = null,
    val rawOutput: String = ""
)

class ShizukuController(private val context: Context) {

    private val permissionListeners = mutableListOf<(Boolean) -> Unit>()
    private val requestListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE_SHIZUKU) {
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            val listeners = synchronized(permissionListeners) {
                val copy = permissionListeners.toList()
                permissionListeners.clear()
                copy
            }
            listeners.forEach { it(granted) }
        }
    }

    private val newProcessMethod: Method? by lazy {
        try {
            Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }
        } catch (_: Throwable) {
            null
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        val granted = isPermissionGranted()
        val listeners = synchronized(permissionListeners) {
            val copy = permissionListeners.toList()
            permissionListeners.clear()
            copy
        }
        listeners.forEach { it(granted) }
    }

    init {
        try {
            Shizuku.addRequestPermissionResultListener(requestListener)
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        } catch (_: Throwable) {}
    }

    fun cleanup() {
        try {
            Shizuku.removeRequestPermissionResultListener(requestListener)
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
        } catch (_: Throwable) {}
        synchronized(permissionListeners) {
            permissionListeners.clear()
        }
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
    }

    fun isPermissionGranted(): Boolean {
        return try {
            if (!isShizukuAvailable()) return false
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    fun requestPermission(onResult: (Boolean) -> Unit) {
        if (!isShizukuAvailable()) {
            openShizukuApp()
            onResult(false)
            return
        }
        if (isPermissionGranted()) {
            onResult(true)
            return
        }
        try {
            synchronized(permissionListeners) {
                permissionListeners.add(onResult)
            }
            Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
        } catch (_: Throwable) {
            onResult(false)
        }
    }

    fun openShizukuApp() {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        }
    }

    private var cachedRootAvailable: Boolean? = null

    fun isRootAvailable(): Boolean {
        cachedRootAvailable?.let { return it }
        val available = try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = BufferedReader(InputStreamReader(p.inputStream))
            val line = reader.readLine()
            p.waitFor() == 0 && (line?.contains("uid=0") == true)
        } catch (_: Throwable) {
            false
        }
        cachedRootAvailable = available
        return available
    }

    fun isExecutionReady(): Boolean = isPermissionGranted() || isRootAvailable()

    private fun spawnProcess(cmd: Array<String>): Process {
        if (isShizukuAvailable() && isPermissionGranted() && newProcessMethod != null) {
            return newProcessMethod!!.invoke(null, cmd, null, null) as Process
        }
        if (isRootAvailable()) {
            val cmdStr = cmd.joinToString(" ")
            return Runtime.getRuntime().exec(arrayOf("su", "-c", cmdStr))
        }
        val method = newProcessMethod
            ?: throw IllegalStateException("Neither Shizuku nor Root is connected")
        return method.invoke(null, cmd, null, null) as Process
    }

    /**
     * Executes shell command via Shizuku binder or Root and emits log lines in real-time.
     */
    fun executeCommand(commandArgs: Array<String>): Flow<String> = flow {
        if (!isExecutionReady()) {
            emit("ERROR: Shizuku permission not granted and Root not available.")
            return@flow
        }

        try {
            val process = spawnProcess(commandArgs)
            val reader = BufferedReader(InputStreamReader(process.inputStream), 8192)
            val errorReader = BufferedReader(InputStreamReader(process.errorStream), 8192)

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { emit(it) }
            }
            while (errorReader.readLine().also { line = it } != null) {
                line?.let { emit("ERR: $it") }
            }

            process.waitFor()
            val exitCode = process.exitValue()
            if (exitCode != 0) {
                emit("Exited with code $exitCode")
            }
        } catch (e: Exception) {
            emit("EXCEPTION: ${e.localizedMessage}")
        }
    }.flowOn(Dispatchers.IO)

    // Tracks whether this device supports modern ART Service flags (--full, -p PRIORITY_INTERACTIVE_FAST)
    private var supportsModernArtFlags: Boolean? = null

    /**
     * Executes dexopt compilation with modern ART flags (--full and -p PRIORITY_INTERACTIVE_FAST)
     * on Android 14+ / S24 Ultra / modern One UI, with automatic seamless fallback for legacy Android.
     */
    fun compilePackage(packageName: String, mode: String): Flow<String> = flow {
        if (!isExecutionReady()) {
            emit("ERROR: Shizuku permission not granted and Root not available.")
            return@flow
        }

        if (supportsModernArtFlags == false) {
            val legacyCmd = arrayOf("cmd", "package", "compile", "-m", mode, "-f", packageName)
            executeCommand(legacyCmd).collect { emit(it) }
            return@flow
        }

        val modernCmd = arrayOf(
            "cmd", "package", "compile",
            "-m", mode,
            "-f",
            "--full",
            "-p", "PRIORITY_INTERACTIVE_FAST",
            packageName
        )

        var hasUnknownOption = false
        executeCommand(modernCmd).collect { line ->
            if (line.contains("Unknown option", ignoreCase = true) || line.contains("Error: Unknown", ignoreCase = true)) {
                hasUnknownOption = true
            } else {
                emit(line)
            }
        }

        if (hasUnknownOption) {
            supportsModernArtFlags = false
            emit("[COMPATIBILITY] Modern flags not supported by device runtime. Falling back to standard dexopt...")
            val legacyCmd = arrayOf("cmd", "package", "compile", "-m", mode, "-f", packageName)
            executeCommand(legacyCmd).collect { emit(it) }
        } else {
            supportsModernArtFlags = true
        }
    }.flowOn(Dispatchers.IO)

    fun mapFilterToStatus(filter: String?): CompileStatus {
        if (filter.isNullOrBlank()) return CompileStatus.UNKNOWN
        val f = filter.lowercase().trim()
        return when {
            f == "speed" -> CompileStatus.SPEED
            f == "speed-profile" || f == "space-profile" -> CompileStatus.SPEED_PROFILE
            f == "everything" -> CompileStatus.EVERYTHING
            f in listOf("verify", "quicken", "space", "extract", "run-from-apk", "interpret-only", "assume-verified", "error") -> CompileStatus.UNCOMPILED
            f.contains("speed-profile") -> CompileStatus.SPEED_PROFILE
            f.contains("speed") -> CompileStatus.SPEED
            f.contains("everything") -> CompileStatus.EVERYTHING
            f.contains("verify") || f.contains("interpret") || f.contains("extract") || f.contains("quicken") -> CompileStatus.UNCOMPILED
            else -> CompileStatus.UNKNOWN
        }
    }

    /**
     * Directly streams and parses `dumpsys package dexopt` with zero intermediate array allocation.
     */
    suspend fun getDexoptDumpMap(): Map<String, DexoptDetails> = withContext(Dispatchers.IO) {
        if (!isExecutionReady()) return@withContext emptyMap()
        try {
            val process = spawnProcess(arrayOf("dumpsys", "package", "dexopt"))
            val reader = BufferedReader(InputStreamReader(process.inputStream), 16384)
            val results = parseDexoptStream(reader)
            process.waitFor()
            results
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    /**
     * Parses the dexopt dump directly from a buffered stream without allocating thousands of lines in RAM.
     */
    fun parseDexoptStream(reader: BufferedReader): Map<String, DexoptDetails> {
        val results = mutableMapOf<String, DexoptDetails>()
        var currentPkg: String? = null
        var inBaseApk = false
        var filterFoundForBaseApk = false

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val currentLine = line ?: continue
            val trimmed = currentLine.trim()
            if (trimmed.isEmpty()) continue

            // Fast-path package header: [com.example.app]
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                val candidate = trimmed.substring(1, trimmed.length - 1).trim()
                if (candidate.contains(".") && !candidate.contains(" ")) {
                    currentPkg = candidate
                    inBaseApk = false
                    filterFoundForBaseApk = false
                    continue
                }
            }

            if (currentPkg != null) {
                if (trimmed.startsWith("path:")) {
                    inBaseApk = trimmed.contains("base.apk") || (!trimmed.contains("split_") && trimmed.endsWith(".apk"))
                }

                val filterMatch = FILTER_REGEX.find(trimmed)
                if (filterMatch != null) {
                    val rawFilter = filterMatch.groupValues[1].lowercase()
                    val reason = REASON_REGEX.find(trimmed)?.groupValues?.get(1)
                    val abi = ABI_REGEX.find(trimmed)?.groupValues?.get(1)
                    val odex = ODEX_REGEX.find(trimmed)?.groupValues?.get(1)

                    if (inBaseApk && !filterFoundForBaseApk) {
                        results[currentPkg] = DexoptDetails(
                            packageName = currentPkg,
                            status = mapFilterToStatus(rawFilter),
                            filter = rawFilter,
                            reason = reason,
                            primaryAbi = abi,
                            odexPath = odex,
                            rawOutput = ""
                        )
                        filterFoundForBaseApk = true
                    } else if (!filterFoundForBaseApk && !results.containsKey(currentPkg)) {
                        results[currentPkg] = DexoptDetails(
                            packageName = currentPkg,
                            status = mapFilterToStatus(rawFilter),
                            filter = rawFilter,
                            reason = reason,
                            primaryAbi = abi,
                            odexPath = odex,
                            rawOutput = ""
                        )
                    }
                }
            }
        }
        return results
    }

    /**
     * Backward-compatible parseDexoptDump for lines lists.
     */
    fun parseDexoptDump(lines: List<String>): Map<String, DexoptDetails> {
        val reader = BufferedReader(java.io.StringReader(lines.joinToString("\n")), 8192)
        return parseDexoptStream(reader)
    }

    /**
     * Queries definitive, ground-truth ART compilation details for a specific package.
     */
    suspend fun getAppDexoptDetails(packageName: String): DexoptDetails = withContext(Dispatchers.IO) {
        if (!isExecutionReady()) {
            return@withContext DexoptDetails(
                packageName = packageName,
                status = CompileStatus.UNKNOWN,
                filter = "UNKNOWN",
                rawOutput = "Shizuku permission not granted and Root not available."
            )
        }

        val outputLines = mutableListOf<String>()
        try {
            val process = spawnProcess(arrayOf("dumpsys", "package", packageName))
            val reader = BufferedReader(InputStreamReader(process.inputStream), 8192)
            var line: String?
            var inDexoptState = false
            while (reader.readLine().also { line = it } != null) {
                val current = line ?: continue
                if (current.contains("Dexopt state:") || current.contains("Dexopt:")) {
                    inDexoptState = true
                }
                if (inDexoptState) {
                    outputLines.add(current)
                    if (outputLines.size > 1 && current.isNotEmpty() && !current.startsWith(" ") && !current.startsWith("\t")) {
                        break
                    }
                }
            }
            process.waitFor()
        } catch (_: Throwable) {}

        // Fallback: try pm art dump on Android 14+ if dumpsys package did not yield dexopt state
        if (outputLines.isEmpty()) {
            try {
                val process = spawnProcess(arrayOf("pm", "art", "dump", packageName))
                val reader = BufferedReader(InputStreamReader(process.inputStream), 8192)
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { outputLines.add(it) }
                }
                process.waitFor()
            } catch (_: Throwable) {}
        }

        if (outputLines.isEmpty()) {
            return@withContext DexoptDetails(
                packageName = packageName,
                status = CompileStatus.UNKNOWN,
                filter = "UNKNOWN",
                rawOutput = "No dexopt information returned by system."
            )
        }

        val hasHeader = outputLines.any { it.contains("[$packageName]") }
        val parseableLines = if (hasHeader) outputLines else listOf("[$packageName]") + outputLines
        val parsedMap = parseDexoptDump(parseableLines)
        val details = parsedMap[packageName]
        if (details != null) {
            return@withContext details.copy(rawOutput = outputLines.joinToString("\n").trim())
        }

        val rawText = outputLines.joinToString("\n")
        val filterMatch = FILTER_REGEX.find(rawText)

        if (filterMatch != null) {
            val filter = filterMatch.groupValues[1]
            val reason = REASON_REGEX.find(rawText)?.groupValues?.get(1)
            val abi = ABI_REGEX.find(rawText)?.groupValues?.get(1)
            return@withContext DexoptDetails(
                packageName = packageName,
                status = mapFilterToStatus(filter),
                filter = filter,
                reason = reason,
                primaryAbi = abi,
                rawOutput = rawText.trim()
            )
        }

        return@withContext DexoptDetails(
            packageName = packageName,
            status = CompileStatus.UNKNOWN,
            filter = "UNKNOWN",
            rawOutput = rawText.trim()
        )
    }

    /**
     * Checks compilation status for an app package.
     */
    suspend fun getAppCompilationStatus(packageName: String): String = withContext(Dispatchers.IO) {
        val details = getAppDexoptDetails(packageName)
        if (details.status != CompileStatus.UNKNOWN) {
            return@withContext "${details.status.label} (${details.filter})"
        }
        return@withContext "UNKNOWN"
    }

    companion object {
        const val REQUEST_CODE_SHIZUKU = 9021
        private val FILTER_REGEX = Regex("""(?:\[status=|status=|compilation_filter=|filter:\s*|status:\s*compiled\s*\()([a-zA-Z0-9_-]+)""")
        private val REASON_REGEX = Regex("""(?:\[reason=|reason=|compilation reason:\s*)([a-zA-Z0-9_-]+)""")
        private val ABI_REGEX = Regex("""\b(arm64(?:-v8a)?|armeabi(?:-v7a)?|x86_64|x86)\b""")
        private val ODEX_REGEX = Regex("""(?:artifact:?\s*|oat file:?\s*)([^\s\]]+)""")
    }
}
