package com.antigravity.dexoptx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.antigravity.dexoptx.model.AppInfo
import com.antigravity.dexoptx.model.CompileStatus
import com.antigravity.dexoptx.shizuku.DexoptDetails
import com.antigravity.dexoptx.theme.CyberAmber
import com.antigravity.dexoptx.theme.DarkMidnight
import com.antigravity.dexoptx.theme.ElectricIndigo
import com.antigravity.dexoptx.theme.ElectricViolet
import com.antigravity.dexoptx.theme.GlassBorder
import com.antigravity.dexoptx.theme.GlassSurface
import com.antigravity.dexoptx.theme.NeonEmerald
import com.antigravity.dexoptx.theme.TextMuted
import com.antigravity.dexoptx.theme.TextPrimary

@Composable
fun AppDexoptInspectorDialog(
    app: AppInfo,
    details: DexoptDetails?,
    isLoading: Boolean,
    onRecheck: () -> Unit,
    onCompileSpeed: () -> Unit,
    onCompileProfile: () -> Unit,
    onDismiss: () -> Unit
) {
    var showRawDump by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 560.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(DarkMidnight.copy(alpha = 0.96f))
                .border(1.5.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        AppIconImage(
                            drawable = app.iconDrawable,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = app.appName,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = app.packageName,
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x18FFFFFF))
                            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status Banner
                val currentStatus = details?.status ?: app.compileStatus
                val currentFilter = details?.filter ?: app.compilationFilter ?: "UNKNOWN"

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            when (currentStatus) {
                                CompileStatus.SPEED -> ElectricIndigo.copy(alpha = 0.15f)
                                CompileStatus.SPEED_PROFILE -> NeonEmerald.copy(alpha = 0.15f)
                                CompileStatus.EVERYTHING -> ElectricViolet.copy(alpha = 0.2f)
                                CompileStatus.UNCOMPILED -> CyberAmber.copy(alpha = 0.15f)
                                CompileStatus.UNKNOWN -> Color(0x14FFFFFF)
                            }
                        )
                        .border(
                            1.dp,
                            when (currentStatus) {
                                CompileStatus.SPEED -> ElectricIndigo.copy(alpha = 0.4f)
                                CompileStatus.SPEED_PROFILE -> NeonEmerald.copy(alpha = 0.4f)
                                CompileStatus.EVERYTHING -> ElectricViolet.copy(alpha = 0.5f)
                                CompileStatus.UNCOMPILED -> CyberAmber.copy(alpha = 0.4f)
                                CompileStatus.UNKNOWN -> Color(0x33FFFFFF)
                            },
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "VERIFIED ART STATE",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = ElectricIndigo,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentStatus.label,
                            color = when (currentStatus) {
                                CompileStatus.SPEED -> ElectricIndigo
                                CompileStatus.SPEED_PROFILE -> NeonEmerald
                                CompileStatus.EVERYTHING -> ElectricViolet
                                CompileStatus.UNCOMPILED -> CyberAmber
                                CompileStatus.UNKNOWN -> TextPrimary
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (currentFilter.lowercase()) {
                                "speed" -> "Full Ahead-Of-Time (AOT) machine code compilation for all methods."
                                "speed-profile" -> "Profile-guided AOT compilation. Frequently called methods are compiled to native code."
                                "everything" -> "All code fully compiled with all compiler optimizations enabled."
                                "verify" -> "Bytecode verification only. No native AOT compilation; code executes via interpreter/JIT."
                                "quicken" -> "Bytecode quickened instructions only. Not native machine code."
                                "run-from-apk", "interpret-only" -> "Direct interpretation without ahead-of-time bytecode compilation."
                                else -> "Compilation status queried live from Android Runtime (ART)."
                            },
                            color = TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Detail Attributes Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AttributeCard(
                        title = "COMPILER FILTER",
                        value = details?.filter ?: app.compilationFilter ?: "UNKNOWN",
                        modifier = Modifier.weight(1f)
                    )
                    AttributeCard(
                        title = "COMPILATION REASON",
                        value = details?.reason ?: app.compilationReason ?: "UNKNOWN",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AttributeCard(
                        title = "PRIMARY ARCHITECTURE",
                        value = details?.primaryAbi ?: app.primaryAbi ?: "arm64",
                        modifier = Modifier.weight(1f)
                    )
                    AttributeCard(
                        title = "PACKAGE TYPE",
                        value = if (app.isSystemApp) "SYSTEM APP" else "USER INSTALLED",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (details?.odexPath != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AttributeCard(
                        title = "OAT / ODEX ARTIFACT",
                        value = details.odexPath,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Recheck and Single App Compile (Zero Truncation, TV D-Pad Focusable)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InspectorActionButton(
                        text = "LIVE RE-CHECK ART SUBSYSTEM",
                        icon = Icons.Rounded.Refresh,
                        onClick = onRecheck,
                        enabled = !isLoading,
                        containerColor = Color(0x22FFFFFF),
                        contentColor = Color.White,
                        borderColor = Color(0x44FFFFFF),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InspectorActionButton(
                            text = "COMPILE: PROFILE",
                            icon = Icons.Rounded.Bolt,
                            onClick = onCompileProfile,
                            enabled = !isLoading,
                            containerColor = NeonEmerald.copy(alpha = 0.15f),
                            contentColor = NeonEmerald,
                            borderColor = NeonEmerald.copy(alpha = 0.4f),
                            modifier = Modifier.weight(1f)
                        )

                        InspectorActionButton(
                            text = "COMPILE: SPEED",
                            icon = Icons.Rounded.Speed,
                            onClick = onCompileSpeed,
                            enabled = !isLoading,
                            containerColor = ElectricIndigo.copy(alpha = 0.25f),
                            contentColor = Color.White,
                            borderColor = ElectricIndigo.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Raw Dumpsys Terminal Box Toggle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .clickable { showRawDump = !showRawDump }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Terminal,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (showRawDump) "HIDE RAW ART DUMPSYS" else "VIEW RAW ART DUMPSYS",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (showRawDump) "▲" else "▼",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                if (showRawDump && details?.rawOutput != null && details.rawOutput.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF07090E))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = details.rawOutput,
                            color = Color(0xFFA5B4FC),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttributeCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GlassSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = title,
                color = TextMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun InspectorActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: () -> Unit,
    enabled: Boolean,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color = GlassBorder,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val border = if (isFocused) Color(0xFF38BDF8) else borderColor
    val bg = if (isFocused) Color(0x4038BDF8) else containerColor
    val borderWidth = if (isFocused) 2.5.dp else 1.dp

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(borderWidth, border, RoundedCornerShape(12.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isFocused) Color.White else contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = if (isFocused) Color.White else contentColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
        }
    }
}
