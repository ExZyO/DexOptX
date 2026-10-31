package com.antigravity.dexoptx.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.antigravity.dexoptx.model.AppInfo
import com.antigravity.dexoptx.model.CompileStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    suspend fun loadInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val packages = try {
            packageManager.getInstalledPackages(0)
        } catch (_: Throwable) {
            emptyList()
        }

        if (packages.isEmpty()) return@withContext emptyList()

        // Distribute label reading across CPU cores in parallel chunks for instantaneous loading
        val cpuCores = Runtime.getRuntime().availableProcessors().coerceIn(2, 8)
        val chunkSize = (packages.size + cpuCores - 1) / cpuCores

        coroutineScope {
            packages.chunked(chunkSize).map { chunk ->
                async(Dispatchers.Default) {
                    chunk.mapNotNull { pkg ->
                        val appInfo = pkg.applicationInfo ?: return@mapNotNull null
                        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                        val appName = try {
                            appInfo.loadLabel(packageManager).toString()
                        } catch (_: Throwable) {
                            pkg.packageName
                        }

                        AppInfo(
                            packageName = pkg.packageName,
                            appName = appName,
                            isSystemApp = isSystem,
                            lastUpdateTime = pkg.lastUpdateTime,
                            compileStatus = CompileStatus.UNKNOWN,
                            isSelected = false
                        )
                    }
                }
            }.awaitAll().flatten()
        }.sortedBy { it.appName.lowercase() }
    }
}
