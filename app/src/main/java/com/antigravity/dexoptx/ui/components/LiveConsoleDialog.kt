package com.antigravity.dexoptx.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.antigravity.dexoptx.theme.DarkMidnight
import com.antigravity.dexoptx.theme.ElectricIndigo
import com.antigravity.dexoptx.theme.ElectricViolet
import com.antigravity.dexoptx.theme.GlassBorder
import com.antigravity.dexoptx.theme.NeonEmerald
import com.antigravity.dexoptx.theme.RadiantFuchsia
import com.antigravity.dexoptx.theme.TextMuted
import com.antigravity.dexoptx.theme.TextPrimary

@Composable
fun LiveConsoleDialog(
    isRunning: Boolean,
    currentApp: String,
    currentIndex: Int,
    totalCount: Int,
    logs: List<String>,
    onDismiss: () -> Unit,
    onCancel: () -> Unit = {}
) {
    val progress = if (totalCount > 0) currentIndex.toFloat() / totalCount else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progressAnim")
    val listState = rememberLazyListState()

    val isStopped = !isRunning && currentIndex < totalCount && logs.any { it.contains("STOPPED", ignoreCase = true) }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(DarkMidnight.copy(alpha = 0.95f))
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when {
                                isRunning -> Icons.Rounded.Terminal
                                isStopped -> Icons.Rounded.Close
                                else -> Icons.Rounded.CheckCircle
                            },
                            contentDescription = null,
                            tint = when {
                                isRunning -> ElectricViolet
                                isStopped -> Color(0xFFF87171)
                                else -> NeonEmerald
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isRunning -> "COMPILING APPS..."
                                isStopped -> "COMPILATION STOPPED"
                                else -> "OPTIMIZATION COMPLETED"
                            },
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "$currentIndex / $totalCount",
                        color = if (isStopped) Color(0xFFF87171) else ElectricViolet,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Smooth Progress Bar
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isStopped) Color(0xFFF87171) else ElectricIndigo,
                    trackColor = Color(0x22FFFFFF)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Current app status
                Text(
                    text = when {
                        isRunning -> "ACTIVE: $currentApp"
                        isStopped -> "STOPPED AT $currentIndex OF $totalCount APPS"
                        else -> "ALL $totalCount APPS COMPILED"
                    },
                    color = when {
                        isRunning -> Color(0xFFE2E8F0)
                        isStopped -> Color(0xFFF87171)
                        else -> NeonEmerald
                    },
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Live Terminal Stream Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF030305))
                        .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    LazyColumn(state = listState) {
                        items(logs) { logLine ->
                            Text(
                                text = logLine,
                                color = if (logLine.startsWith("ERR") || logLine.startsWith("EXCEPTION") || logLine.contains("[STOPPED]")) Color(0xFFF87171) else TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Button: STOP COMPILATION & MINIMIZE when running, DONE when idle/completed
                if (isRunning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onCancel,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "STOP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x22FFFFFF),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "MINIMIZE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricIndigo,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DONE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
