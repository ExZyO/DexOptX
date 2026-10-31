package com.antigravity.dexoptx.model

enum class CompileStatus(val label: String) {
    SPEED("COMPILED: SPEED"),
    SPEED_PROFILE("COMPILED: PROFILE"),
    EVERYTHING("COMPILED: EVERYTHING"),
    UNCOMPILED("UNCOMPILED"),
    UNKNOWN("STATUS: UNKNOWN");

    val isCompiled: Boolean
        get() = this == SPEED || this == SPEED_PROFILE || this == EVERYTHING

    val isSpeedOptimized: Boolean
        get() = this == SPEED || this == EVERYTHING

    val isProfileOptimized: Boolean
        get() = this == SPEED_PROFILE
}

data class StatusCounts(
    val total: Int = 0,
    val uncompiled: Int = 0,
    val compiled: Int = 0,
    val speed: Int = 0,
    val profile: Int = 0,
    val recent: Int = 0
)

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val lastUpdateTime: Long,
    val compileStatus: CompileStatus = CompileStatus.UNKNOWN,
    val compilationFilter: String? = null,
    val compilationReason: String? = null,
    val primaryAbi: String? = null,
    val isSelected: Boolean = false
) {
    val isSpeedOptimized: Boolean
        get() = compileStatus.isSpeedOptimized ||
                (compilationFilter != null && compilationFilter.lowercase().contains("speed") && !compilationFilter.lowercase().contains("profile"))

    val isProfileOptimized: Boolean
        get() = compileStatus.isProfileOptimized ||
                (compilationFilter != null && compilationFilter.lowercase().contains("profile"))
}
