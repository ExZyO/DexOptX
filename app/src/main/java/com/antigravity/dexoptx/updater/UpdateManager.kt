package com.antigravity.dexoptx.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.antigravity.dexoptx.shizuku.ShizukuController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val isNewer: Boolean,
    val isBuilding: Boolean = false,
    val latestVersion: String,
    val currentVersion: String,
    val apkUrl: String,
    val releasePage: String,
    val actionsUrl: String = "https://github.com/ExZyO/DexOptX/actions",
    val releaseNotes: String = ""
)

class UpdateManager(private val context: Context) {

    val currentVersion: String
        get() = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.8"
        } catch (_: Exception) {
            "1.0.8"
        }

    private fun httpGet(urlStr: String, accept: String = "application/json"): String? {
        return try {
            val separator = if (urlStr.contains("?")) "&" else "?"
            val url = URL("$urlStr${separator}t=${System.currentTimeMillis()}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", accept)
                setRequestProperty("User-Agent", "DexOptX-Updater")
                connectTimeout = 6000
                readTimeout = 6000
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun checkForUpdates(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        coroutineScope {
            try {
                var releaseNotes = ""
                var discoveredApkUrl: String? = null
                var latestReleaseTag: String? = null
                var releasePageUrl = "https://github.com/ExZyO/DexOptX/releases"
                val candidateVersions = mutableListOf<String>()

                // P1: GitHub Releases Latest API
                val p1 = async {
                    val raw = httpGet(
                        "https://api.github.com/repos/ExZyO/DexOptX/releases/latest",
                        "application/vnd.github.v3+json"
                    )
                    if (raw != null) {
                        try {
                            val json = JSONObject(raw)
                            val body = json.optString("body", "")
                            if (body.isNotEmpty()) releaseNotes = body
                            val tag = json.optString("tag_name", "").removePrefix("v").trim()
                            val name = json.optString("name", "").removePrefix("v").trim()
                            releasePageUrl = json.optString("html_url", releasePageUrl)

                            val tagVer = parseVersionString(tag)
                            val nameVer = parseVersionString(name)
                            if (tagVer != null) {
                                candidateVersions.add(tagVer)
                                latestReleaseTag = tag
                            }
                            if (nameVer != null) candidateVersions.add(nameVer)

                            val assets = json.optJSONArray("assets")
                            if (assets != null) {
                                val deviceAbi = deviceAbiTag()
                                var archMatch: String? = null
                                var anyApk: String? = null
                                for (i in 0 until assets.length()) {
                                    val asset = assets.getJSONObject(i)
                                    val aName = asset.optString("name", "")
                                    if (aName.endsWith(".apk", ignoreCase = true)) {
                                        if (anyApk == null) anyApk = asset.optString("browser_download_url", "")
                                        if (aName.contains(deviceAbi, ignoreCase = true)) {
                                            archMatch = asset.optString("browser_download_url", "")
                                        }
                                    }
                                }
                                discoveredApkUrl = archMatch ?: anyApk
                            }
                        } catch (_: Exception) {}
                    }
                }

                // P2: Raw GitHub version.json (Fast bypass of API rate limits & cache)
                val p2 = async {
                    val raw = httpGet("https://raw.githubusercontent.com/ExZyO/DexOptX/main/version.json")
                    if (raw != null) {
                        try {
                            val json = JSONObject(raw)
                            val ver = json.optString("version", "").removePrefix("v").trim()
                            val apk = pickApkUrlForDevice(json)
                            val verClean = parseVersionString(ver)
                            if (verClean != null) candidateVersions.add(verClean)
                            if (apk.isNotEmpty() && discoveredApkUrl.isNullOrEmpty()) {
                                discoveredApkUrl = apk
                            }
                        } catch (_: Exception) {}
                    }
                }

                // P3: jsDelivr CDN version.json edge cache mirror
                val p3 = async {
                    val raw = httpGet("https://cdn.jsdelivr.net/gh/ExZyO/DexOptX@main/version.json")
                    if (raw != null) {
                        try {
                            val json = JSONObject(raw)
                            val ver = json.optString("version", "").removePrefix("v").trim()
                            val apk = pickApkUrlForDevice(json)
                            val verClean = parseVersionString(ver)
                            if (verClean != null) candidateVersions.add(verClean)
                            if (apk.isNotEmpty() && discoveredApkUrl.isNullOrEmpty()) {
                                discoveredApkUrl = apk
                            }
                        } catch (_: Exception) {}
                    }
                }

                // P4: GitHub Actions Workflow Run Status (detect active in-progress builds)
                var actionsBuilding = false
                var actionsHtmlUrl = "https://github.com/ExZyO/DexOptX/actions"
                val p4 = async {
                    val raw = httpGet(
                        "https://api.github.com/repos/ExZyO/DexOptX/actions/runs?per_page=5",
                        "application/vnd.github.v3+json"
                    )
                    if (raw != null) {
                        try {
                            val json = JSONObject(raw)
                            val runs = json.optJSONArray("workflow_runs")
                            if (runs != null && runs.length() > 0) {
                                for (i in 0 until runs.length()) {
                                    val run = runs.getJSONObject(i)
                                    val status = run.optString("status", "")
                                    if (status == "in_progress" || status == "queued") {
                                        actionsBuilding = true
                                        actionsHtmlUrl = run.optString("html_url", actionsHtmlUrl)
                                        break
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }

                p1.await()
                p2.await()
                p3.await()
                p4.await()

                if (candidateVersions.isEmpty()) {
                    return@coroutineScope Result.failure(Exception("Could not retrieve latest version information."))
                }

                // Sort descending to find the highest remote version
                candidateVersions.sortWith { a, b -> compareVersions(b, a) }
                val remoteVersion = candidateVersions.first()

                val isNewer = compareVersions(remoteVersion, currentVersion) > 0
                val targetTag = latestReleaseTag ?: "v$remoteVersion"
                val formattedTag = if (targetTag.startsWith("v", ignoreCase = true)) targetTag else "v$targetTag"

                val apkUrl = discoveredApkUrl
                val isApkUploaded = !apkUrl.isNullOrEmpty() && (apkUrl.contains(remoteVersion) || apkUrl.contains(targetTag))
                val isBuilding = isNewer && (!isApkUploaded || actionsBuilding)

                val targetApkUrl = if (isApkUploaded && apkUrl != null) {
                    apkUrl
                } else {
                    "https://github.com/ExZyO/DexOptX/releases/download/$formattedTag/DexOptX-$formattedTag-${deviceAbiTag()}-release.apk"
                }

                Result.success(
                    UpdateInfo(
                        isNewer = isNewer,
                        isBuilding = isBuilding,
                        latestVersion = formattedTag,
                        currentVersion = "v$currentVersion",
                        apkUrl = if (isBuilding) actionsHtmlUrl else targetApkUrl,
                        releasePage = releasePageUrl,
                        actionsUrl = actionsHtmlUrl,
                        releaseNotes = releaseNotes
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun downloadApkDirectly(
        urlStr: String,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            var currentUrl = urlStr
            var conn: HttpURLConnection? = null
            var redirects = 0
            while (redirects < 5) {
                val url = URL(currentUrl)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "DexOptX-Updater")
                }
                val status = conn.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (!location.isNullOrEmpty()) {
                        currentUrl = location
                        redirects++
                        continue
                    }
                }
                break
            }

            val actualConn = conn ?: throw IllegalStateException("Could not open connection to $urlStr")
            val totalBytes = actualConn.contentLengthLong
            val updateDir = File(context.cacheDir, "apk_updates").apply { if (!exists()) mkdirs() }
            val apkFile = File(updateDir, "DexOptX-latest.apk")
            if (apkFile.exists()) apkFile.delete()

            actualConn.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        onProgress(totalRead, totalBytes)
                    }
                    output.flush()
                }
            }
            actualConn.disconnect()
            apkFile.setReadable(true, false)
            Result.success(apkFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun installApk(
        apkFile: File,
        shizukuController: ShizukuController
    ): Result<String> = withContext(Dispatchers.IO) {
        // Method A: If Shizuku or Root is connected, perform seamless installation via shell
        if (shizukuController.isExecutionReady()) {
            try {
                val tmpPath = "/data/local/tmp/DexOptX-update.apk"
                // Copy to public /data/local/tmp/ so Shizuku shell has full read permissions
                shizukuController.executeCommand(arrayOf("cp", apkFile.absolutePath, tmpPath)).collect { _ -> }

                var installOutput = ""
                shizukuController.executeCommand(
                    arrayOf("cmd", "package", "install", "-r", tmpPath)
                ).collect { line ->
                    installOutput += "$line\n"
                }

                if (installOutput.contains("Success", ignoreCase = true)) {
                    return@withContext Result.success("Installed successfully via Shizuku")
                }
            } catch (_: Throwable) {}
        }

        // Method B: Native Android PackageInstaller prompt (In-place modal, no browser)
        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Launched system package installer")
        } catch (e: Exception) {
            openDownloadUrl(apkFile.absolutePath)
            Result.failure(e)
        }
    }

    fun openDownloadUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Throwable) {}
    }

    private fun deviceAbiTag(): String {
        val primary = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        return when {
            primary.contains("arm64") -> "arm64-v8a"
            primary.contains("armeabi") -> "armeabi-v7a"
            else -> primary
        }
    }

    private fun pickApkUrlForDevice(json: JSONObject): String {
        val primary = Build.SUPPORTED_ABIS.firstOrNull() ?: ""
        // If device is 32-bit ARM and a v7a URL is available, prefer that
        if (primary.contains("armeabi") || !primary.contains("arm64")) {
            val v7aUrl = json.optString("apkUrlV7a", "")
            if (v7aUrl.isNotEmpty()) return v7aUrl
        }
        // Otherwise use the default (arm64) URL
        return json.optString("apkUrl", json.optString("downloadUrl", ""))
    }

    private fun parseVersionString(input: String): String? {
        val match = Regex("""v?(\d+\.\d+(\.\d+)?)""").find(input)
        return match?.groupValues?.get(1)
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.replace(Regex("[^0-9.]"), "").split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.replace(Regex("[^0-9.]"), "").split(".").map { it.toIntOrNull() ?: 0 }
        val length = maxOf(parts1.size, parts2.size)
        for (i in 0 until length) {
            val num1 = parts1.getOrElse(i) { 0 }
            val num2 = parts2.getOrElse(i) { 0 }
            if (num1 != num2) return num1.compareTo(num2)
        }
        return 0
    }
}
