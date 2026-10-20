package com.antigravity.dexoptx.model

import android.graphics.drawable.Drawable

enum class CompileStatus(val label: String) {
    SPEED("COMPILED: SPEED"),
    SPEED_PROFILE("COMPILED: PROFILE"),
    EVERYTHING("COMPILED: EVERYTHING"),
    UNCOMPILED("UNCOMPILED"),
    UNKNOWN("STATUS: UNKNOWN");

    val isCompiled: Boolean
        get() = this == SPEED || this == SPEED_PROFILE || this == EVERYTHING
}

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val lastUpdateTime: Long,
    val compileStatus: CompileStatus = CompileStatus.UNKNOWN,
    val compilationFilter: String? = null,
    val compilationReason: String? = null,
    val primaryAbi: String? = null,
    val isSelected: Boolean = false,
    val iconDrawable: Drawable? = null
)
