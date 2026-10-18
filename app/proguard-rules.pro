# ProGuard / R8 Rules for DexOptX

# Keep Shizuku IPC interfaces and reflection targets
-keep class rikka.shizuku.** { *; }
-keep interface rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-keep interface moe.shizuku.** { *; }

# Keep App models and reflection
-keep class com.antigravity.dexoptx.model.** { *; }
-keep class com.antigravity.dexoptx.shizuku.** { *; }

# Compose rules
-keepclassmembers class * extends androidx.compose.runtime.State { *; }
