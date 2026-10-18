package com.antigravity.dexoptx.shizuku

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Method

class ShizukuController(private val context: Context) {

    private val permissionListeners = mutableListOf<(Boolean) -> Unit>()
    private val requestListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE_SHIZUKU) {
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            permissionListeners.forEach { it(granted) }
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
        permissionListeners.forEach { it(isPermissionGranted()) }
    }

    init {
        try {
            Shizuku.addRequestPermissionResultListener(requestListener)
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        } catch (_: Throwable) {}
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
            // If Shizuku is not running, launch Shizuku so user can start it
            openShizukuApp()
            onResult(false)
            return
        }
        if (isPermissionGranted()) {
            onResult(true)
            return
        }
        try {
            permissionListeners.add(onResult)
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

    fun isRootAvailable(): Boolean {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = BufferedReader(InputStreamReader(p.inputStream))
            val line = reader.readLine()
            p.waitFor() == 0 && (line?.contains("uid=0") == true)
        } catch (_: Throwable) {
            false
        }
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
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

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

    /**
     * Checks compilation status for an app package.
     */
    suspend fun getAppCompilationStatus(packageName: String): String = withContext(Dispatchers.IO) {
        if (!isPermissionGranted()) return@withContext "UNKNOWN"
        try {
            val process = spawnProcess(arrayOf("dumpsys", "package", packageName))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val current = line ?: continue
                if (current.contains("status=") || current.contains("compilation_filter=")) {
                    return@withContext current.trim()
                }
            }
            process.waitFor()
        } catch (_: Throwable) {}
        return@withContext "UNKNOWN"
    }

    companion object {
        const val REQUEST_CODE_SHIZUKU = 9021
    }
}
