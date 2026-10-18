package com.antigravity.dexoptx.model

import android.graphics.drawable.Drawable

enum class CompileStatus(val label: String) {
    SPEED("COMPILED: SPEED"),
    SPEED_PROFILE("COMPILED: PROFILE"),
    UNCOMPILED("UNCOMPILED"),
    UNKNOWN("STATUS: UNKNOWN")
}

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val lastUpdateTime: Long,
    val compileStatus: CompileStatus = CompileStatus.UNKNOWN,
    val isSelected: Boolean = false,
    val iconDrawable: Drawable? = null
)
