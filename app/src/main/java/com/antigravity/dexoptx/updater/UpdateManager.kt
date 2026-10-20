package com.antigravity.dexoptx.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
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
            pInfo.versionName ?: "1.0.6"
        } catch (_: Exception) {
            "1.0.6"
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
                                for (i in 0 until assets.length()) {
                                    val asset = assets.getJSONObject(i)
                                    val aName = asset.optString("name", "")
                                    if (aName.endsWith(".apk", ignoreCase = true)) {
                                        discoveredApkUrl = asset.optString("browser_download_url", "")
                                        break
                                    }
                                }
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
                            val apk = json.optString("apkUrl", json.optString("downloadUrl", ""))
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
                            val apk = json.optString("apkUrl", json.optString("downloadUrl", ""))
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
                    "https://github.com/ExZyO/DexOptX/releases/download/$formattedTag/DexOptX-$formattedTag-release.apk"
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

    fun openDownloadUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Throwable) {}
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
