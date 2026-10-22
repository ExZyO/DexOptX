package com.antigravity.dexoptx

import android.os.Build

object AppVersion {
    const val VERSION_NAME = "1.0.8"

    val ARCH: String
        get() {
            val primary = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
            return when {
                primary.contains("arm64") -> "ARM64-v8a"
                primary.contains("armeabi") -> "ARM-v7a"
                else -> primary.uppercase()
            }
        }

    val LABEL: String get() = "v$VERSION_NAME • $ARCH"
    val TV_LABEL: String get() = "Nvidia Shield • v$VERSION_NAME • $ARCH"
}
