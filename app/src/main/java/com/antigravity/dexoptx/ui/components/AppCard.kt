package com.antigravity.dexoptx.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.dexoptx.model.AppInfo
import com.antigravity.dexoptx.model.CompileStatus
import com.antigravity.dexoptx.theme.CyberAmber
import com.antigravity.dexoptx.theme.ElectricIndigo
import com.antigravity.dexoptx.theme.GlassBorder
import com.antigravity.dexoptx.theme.GlassSurface
import com.antigravity.dexoptx.theme.NeonEmerald
import com.antigravity.dexoptx.theme.TextMuted
import com.antigravity.dexoptx.theme.TextPrimary
import com.antigravity.dexoptx.theme.TvFocusCardBackground
import com.antigravity.dexoptx.theme.TvFocusHighlight

@Composable
fun AppCard(
    app: AppInfo,
    onToggleSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(targetValue = if (isFocused) 1.04f else 1.0f, label = "cardScale")
    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused && app.isSelected -> Color(0xFFFFFFFF) // Brilliant pure white for focused + selected
            isFocused -> Color(0xFF38BDF8) // Ultra-bright electric cyan focus ring for TV
            app.isSelected -> Color(0xFF818CF8) // Bright neon border for selected
            else -> GlassBorder
        },
        label = "borderColor"
    )
    val borderWidth = when {
        isFocused && app.isSelected -> 4.dp
        isFocused -> 3.5.dp
        app.isSelected -> 2.5.dp
        else -> 1.dp
    }
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused && app.isSelected -> Color(0x666366F1)
            isFocused -> Color(0x4038BDF8) // Glowing cyan surface when focused
            app.isSelected -> Color(0x336366F1) // Clear luminous indigo fill for selected cards
            else -> GlassSurface
        },
        label = "bgColor"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onToggleSelect()
            }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            AppIconImage(
                drawable = app.iconDrawable,
                modifier = Modifier.size(46.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            // App Name & Package Info (Full Names with wrapping, NO ellipsis cutoffs)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = app.appName,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 19.sp,
                        maxLines = 3,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isFocused && app.isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF6366F1))
                                .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SELECTED • FOCUS",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (isFocused) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF0284C7))
                                .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "FOCUSED",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (app.isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF818CF8).copy(alpha = 0.25f))
                                .border(1.dp, Color(0xFF818CF8), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SELECTED",
                                color = Color(0xFFA5B4FC),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = app.packageName,
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Status Badge & Selection Indicator
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                StatusBadge(status = app.compileStatus)

                Spacer(modifier = Modifier.height(8.dp))

                // Custom Checkbox Vector Indicator (Zero emojis, high contrast)
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (app.isSelected) Color(0xFF6366F1) else Color(0x1AFFFFFF)
                        )
                        .border(
                            width = if (app.isSelected) 2.dp else 1.dp,
                            color = if (app.isSelected) Color(0xFFA5B4FC) else Color(0x4DFFFFFF),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (app.isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: CompileStatus) {
    val (label, bg, border, textColor) = when (status) {
        CompileStatus.SPEED -> Quad(
            "COMPILED: SPEED",
            ElectricIndigo.copy(alpha = 0.15f),
            ElectricIndigo.copy(alpha = 0.4f),
            ElectricIndigo
        )
        CompileStatus.SPEED_PROFILE -> Quad(
            "COMPILED: PROFILE",
            NeonEmerald.copy(alpha = 0.15f),
            NeonEmerald.copy(alpha = 0.4f),
            NeonEmerald
        )
        CompileStatus.EVERYTHING -> Quad(
            "COMPILED: ALL",
            ElectricIndigo.copy(alpha = 0.2f),
            ElectricIndigo.copy(alpha = 0.6f),
            ElectricIndigo
        )
        CompileStatus.UNCOMPILED -> Quad(
            "UNCOMPILED",
            CyberAmber.copy(alpha = 0.15f),
            CyberAmber.copy(alpha = 0.4f),
            CyberAmber
        )
        CompileStatus.UNKNOWN -> Quad(
            "READY",
            Color(0x18FFFFFF),
            Color(0x33FFFFFF),
            Color(0xFFCBD5E1)
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun AppIconImage(drawable: Drawable?, modifier: Modifier = Modifier) {
    if (drawable == null) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x266366F1)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Android,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    } else {
        val bitmap = remember(drawable) {
            val width = drawable.intrinsicWidth.coerceAtLeast(1)
            val height = drawable.intrinsicHeight.coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp.asImageBitmap()
        }
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(12.dp))
        )
    }
}
