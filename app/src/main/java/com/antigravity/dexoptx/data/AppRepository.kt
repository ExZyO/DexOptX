package com.antigravity.dexoptx.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.antigravity.dexoptx.model.AppInfo
import com.antigravity.dexoptx.model.CompileStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    suspend fun loadInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val packages = packageManager.getInstalledPackages(0)
        val appList = mutableListOf<AppInfo>()

        for (pkg in packages) {
            val appName = try {
                pkg.applicationInfo?.loadLabel(packageManager)?.toString() ?: pkg.packageName
            } catch (_: Throwable) {
                pkg.packageName
            }

            val isSystem = (pkg.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
            val icon = try {
                pkg.applicationInfo?.loadIcon(packageManager)
            } catch (_: Throwable) {
                null
            }

            appList.add(
                AppInfo(
                    packageName = pkg.packageName,
                    appName = appName,
                    isSystemApp = isSystem,
                    lastUpdateTime = pkg.lastUpdateTime,
                    compileStatus = CompileStatus.UNKNOWN,
                    isSelected = false,
                    iconDrawable = icon
                )
            )
        }

        appList.sortedBy { it.appName.lowercase() }
    }
}
