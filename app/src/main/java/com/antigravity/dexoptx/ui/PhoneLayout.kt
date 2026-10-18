package com.antigravity.dexoptx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.dexoptx.theme.ElectricIndigo
import com.antigravity.dexoptx.theme.GlassBorder
import com.antigravity.dexoptx.theme.GlassSurface
import com.antigravity.dexoptx.theme.NeonEmerald
import com.antigravity.dexoptx.theme.TextMuted
import com.antigravity.dexoptx.theme.TextPrimary
import com.antigravity.dexoptx.ui.components.AppCard
import com.antigravity.dexoptx.ui.components.GlassDock

@Composable
fun PhoneLayout(
    uiState: MainUiState,
    onSearchChange: (String) -> Unit,
    onFilterChange: (AppFilter) -> Unit,
    onToggleApp: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onModeChange: (String) -> Unit,
    onOptimizeClick: () -> Unit,
    onRequestShizuku: () -> Unit,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onDismissUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DEXOPT-X",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "v1.0.2 • 64-bit ARMv8-A",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Check for Updates Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x18FFFFFF))
                            .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                            .clickable { onCheckUpdate() }
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (uiState.isCheckingUpdate) "CHECKING..." else "UPDATE",
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Shizuku Connection Status Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (uiState.isShizukuConnected) NeonEmerald.copy(alpha = 0.15f)
                                else Color(0x33EF4444)
                            )
                            .border(
                                1.dp,
                                if (uiState.isShizukuConnected) NeonEmerald.copy(alpha = 0.4f)
                                else Color(0x66EF4444),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                if (!uiState.isShizukuConnected) onRequestShizuku()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (uiState.isShizukuConnected) "SHIZUKU ACTIVE" else "CONNECT SHIZUKU",
                            color = if (uiState.isShizukuConnected) NeonEmerald else Color(0xFFFCA5A5),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Update Banner (if available)
            if (uiState.availableUpdate != null) {
                com.antigravity.dexoptx.ui.components.UpdateBanner(
                    update = uiState.availableUpdate,
                    onDownloadClick = onDownloadUpdate,
                    onDismissClick = onDismissUpdate,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            } else if (uiState.updateStatusMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x2210B981))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = uiState.updateStatusMessage,
                        color = NeonEmerald,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassSurface)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (uiState.searchQuery.isEmpty()) {
                            Text(
                                text = "Search ${uiState.allApps.size} apps...",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                        BasicTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchChange,
                            singleLine = true,
                            textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                            cursorBrush = SolidColor(ElectricIndigo),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (uiState.searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onSearchChange("") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AppFilter.values()) { filter ->
                    val isSelected = uiState.activeFilter == filter
                    val count = when (filter) {
                        AppFilter.ALL -> uiState.allApps.size
                        AppFilter.USER_ONLY -> uiState.allApps.count { !it.isSystemApp }
                        AppFilter.SYSTEM_ONLY -> uiState.allApps.count { it.isSystemApp }
                        AppFilter.RECENTLY_UPDATED -> uiState.filteredApps.size
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) TextPrimary else GlassSurface
                            )
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .clickable { onFilterChange(filter) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "${filter.label} ($count)",
                            color = if (isSelected) Color(0xFF0F172A) else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // App List
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ElectricIndigo)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = uiState.filteredApps,
                        key = { it.packageName }
                    ) { app ->
                        AppCard(
                            app = app,
                            onToggleSelect = { onToggleApp(app.packageName) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Floating Glass Dock at Bottom
        GlassDock(
            selectedCount = uiState.filteredApps.count { it.isSelected },
            compileMode = uiState.compileMode,
            onModeChange = onModeChange,
            onSelectAll = onSelectAll,
            onDeselectAll = onDeselectAll,
            onOptimizeClick = onOptimizeClick,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
