package com.antigravity.dexoptx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.antigravity.dexoptx.model.CompileStatus
import com.antigravity.dexoptx.theme.DarkMidnight
import com.antigravity.dexoptx.theme.ElectricIndigo
import com.antigravity.dexoptx.theme.ElectricViolet
import com.antigravity.dexoptx.theme.GlassBorder
import com.antigravity.dexoptx.theme.GlassSurface
import com.antigravity.dexoptx.theme.NeonEmerald
import com.antigravity.dexoptx.theme.TextMuted
import com.antigravity.dexoptx.theme.TextPrimary
import com.antigravity.dexoptx.theme.TvFocusHighlight
import com.antigravity.dexoptx.ui.components.AppCard
import com.antigravity.dexoptx.ui.components.CompilationMiniBar
import com.antigravity.dexoptx.ui.components.UpdateBanner

@Composable
fun TvLayout(
    uiState: MainUiState,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (CategoryFilter) -> Unit,
    onStatusFilterChange: (StatusFilter) -> Unit,
    onFilterChange: (AppFilter) -> Unit,
    onToggleApp: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onModeChange: (String) -> Unit,
    onOptimizeClick: () -> Unit,
    onRequestShizuku: () -> Unit,
    onCheckUpdate: () -> Unit = {},
    onDownloadUpdate: () -> Unit = {},
    onDismissUpdate: () -> Unit = {},
    onOpenConsole: () -> Unit = {},
    onCancelOptimization: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedCount = uiState.filteredApps.count { it.isSelected }
    var showTvHelpDialog by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val sevenDaysMs = 7L * 24 * 60 * 60 * 1000

    val catApps = when (uiState.activeCategory) {
        CategoryFilter.ALL -> uiState.allApps
        CategoryFilter.USER_ONLY -> uiState.allApps.filter { !it.isSystemApp }
        CategoryFilter.SYSTEM_ONLY -> uiState.allApps.filter { it.isSystemApp }
    }
    val totalInCat = catApps.size
    val uncompiledInCat = catApps.count {
        it.compileStatus == CompileStatus.UNCOMPILED || (!it.compileStatus.isCompiled && it.compileStatus != CompileStatus.UNKNOWN)
    }
    val compiledInCat = catApps.count { it.compileStatus.isCompiled }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Left Control Panel (290dp width)
        Box(
            modifier = Modifier
                .width(290.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xD90E1017))
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "DEXOPT-X TV",
                        color = ElectricViolet,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Nvidia Shield Pro • v1.0.3",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // TV Check for Updates Button
                    var isCheckUpdateFocused by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x18FFFFFF))
                            .border(
                                width = if (isCheckUpdateFocused) 2.dp else 1.dp,
                                color = if (isCheckUpdateFocused) TvFocusHighlight else GlassBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .focusable()
                            .onFocusChanged { isCheckUpdateFocused = it.isFocused }
                            .onKeyEvent {
                                if (it.key == Key.DirectionCenter || it.key == Key.Enter) {
                                    onCheckUpdate()
                                    true
                                } else false
                            }
                            .clickable { onCheckUpdate() }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = "Check for updates",
                            tint = if (uiState.isCheckingUpdate) ElectricViolet else TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (uiState.isCheckingUpdate) "CHECKING..." else "CHECK UPDATE",
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Shizuku / Root Status Pill + TV Help Guide
                    var isShizukuFocused by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (uiState.isShizukuConnected) NeonEmerald.copy(alpha = 0.15f) else Color(0x33EF4444)
                                )
                                .border(
                                    width = if (isShizukuFocused) 2.5.dp else 1.dp,
                                    color = if (isShizukuFocused) TvFocusHighlight else (if (uiState.isShizukuConnected) NeonEmerald.copy(alpha = 0.4f) else Color(0x66EF4444)),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .focusable()
                                .onFocusChanged { isShizukuFocused = it.isFocused }
                                .onKeyEvent {
                                    if (it.key == Key.DirectionCenter || it.key == Key.Enter) {
                                        if (!uiState.isShizukuConnected) onRequestShizuku()
                                        true
                                    } else false
                                }
                                .clickable { if (!uiState.isShizukuConnected) onRequestShizuku() }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (uiState.isShizukuConnected) "ROOT/SHIZUKU ACTIVE" else "CONNECT SHIZUKU",
                                color = if (uiState.isShizukuConnected) NeonEmerald else Color(0xFFFCA5A5),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // TV Setup Help Button
                        var isHelpFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x22FFFFFF))
                                .border(
                                    width = if (isHelpFocused) 2.dp else 1.dp,
                                    color = if (isHelpFocused) TvFocusHighlight else GlassBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .focusable()
                                .onFocusChanged { isHelpFocused = it.isFocused }
                                .onKeyEvent {
                                    if (it.key == Key.DirectionCenter || it.key == Key.Enter) {
                                        showTvHelpDialog = true
                                        true
                                    } else false
                                }
                                .clickable { showTvHelpDialog = true }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HelpOutline,
                                contentDescription = "Shield TV Setup Guide",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Category Filter (USER APPS / SYSTEM / ALL)
                    Text(
                        text = "APP CATEGORY",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        CategoryFilter.values().forEach { cat ->
                            val isSelected = uiState.activeCategory == cat
                            var isFocused by remember { mutableStateOf(false) }
                            val count = when (cat) {
                                CategoryFilter.ALL -> uiState.allApps.size
                                CategoryFilter.USER_ONLY -> uiState.allApps.count { !it.isSystemApp }
                                CategoryFilter.SYSTEM_ONLY -> uiState.allApps.count { it.isSystemApp }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) ElectricIndigo else Color(0x18FFFFFF))
                                    .border(
                                        width = if (isFocused) 2.dp else 1.dp,
                                        color = if (isFocused) TvFocusHighlight else GlassBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .focusable()
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .onKeyEvent {
                                        if (it.key == Key.DirectionCenter || it.key == Key.Enter) {
                                            onCategoryChange(cat)
                                            true
                                        } else false
                                    }
                                    .clickable { onCategoryChange(cat) }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cat.label,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "$count",
                                        color = if (isSelected) Color(0xFFA5B4FC) else TextMuted,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status Sub-Filter (ALL / UNCOMPILED / COMPILED)
                    Text(
                        text = "STATUS FILTER",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            Triple("ALL", StatusFilter.ALL, totalInCat),
                            Triple("UNOPT", StatusFilter.UNCOMPILED, uncompiledInCat),
                            Triple("OPT", StatusFilter.COMPILED, compiledInCat)
                        ).forEach { (lbl, sf, count) ->
                            val isStatSelected = uiState.activeStatusFilter == sf
                            var isChipFocused by remember { mutableStateOf(false) }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isStatSelected) Color(0xFF334155) else Color(0x18FFFFFF))
                                    .border(
                                        width = if (isChipFocused) 2.dp else 1.dp,
                                        color = if (isChipFocused) TvFocusHighlight else GlassBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .focusable()
                                    .onFocusChanged { isChipFocused = it.isFocused }
                                    .onKeyEvent {
                                        if (it.key == Key.DirectionCenter || it.key == Key.Enter) {
                                            onStatusFilterChange(sf)
                                            true
                                        } else false
                                    }
                                    .clickable { onStatusFilterChange(sf) }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = lbl,
                                        color = if (isStatSelected) Color.White else TextMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "($count)",
                                        color = if (isStatSelected) Color(0xFFA5B4FC) else TextMuted,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // D-Pad Remote Focusable Compile Mode Selector
                    Text(
                        text = "COMPILE PROFILE",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x22FFFFFF))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val isProfile = uiState.compileMode == "speed-profile"
                        var isProfileFocused by remember { mutableStateOf(false) }
                        var isSpeedFocused by remember { mutableStateOf(false) }

                        // PROFILE option (focusable with D-pad)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isProfile) ElectricIndigo else Color.Transparent)
                                .border(
                                    width = if (isProfileFocused) 2.dp else 0.dp,
                                    color = if (isProfileFocused) TvFocusHighlight else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .focusable()
                                .onFocusChanged { isProfileFocused = it.isFocused }
                                .onKeyEvent {
                                    if (it.key == Key.DirectionCenter || it.key == Key.Enter) {
                                        onModeChange("speed-profile")
                                        true
                                    } else false
                                }
                                .clickable { onModeChange("speed-profile") }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PROFILE",
                                color = if (isProfile) Color.White else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // SPEED option (focusable with D-pad)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isProfile) ElectricIndigo else Color.Transparent)
                                .border(
                                    width = if (isSpeedFocused) 2.dp else 0.dp,
                                    color = if (isSpeedFocused) TvFocusHighlight else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .focusable()
                                .onFocusChanged { isSpeedFocused = it.isFocused }
                                .onKeyEvent {
                                    if (it.key == Key.DirectionCenter || it.key == Key.Enter) {
                                        onModeChange("speed")
                                        true
                                    } else false
                                }
                                .clickable { onModeChange("speed") }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SPEED",
                                color = if (!isProfile) Color.White else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Bottom Left Actions
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSelectAll,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF))
                        ) {
                            Text("SELECT ALL", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = onDeselectAll,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF))
                        ) {
                            Text("CLEAR", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    var isOptimizeFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = onOptimizeClick,
                        enabled = selectedCount > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .focusable()
                            .onFocusChanged { isOptimizeFocused = it.isFocused }
                            .border(
                                width = if (isOptimizeFocused) 2.5.dp else 0.dp,
                                color = TvFocusHighlight,
                                shape = RoundedCornerShape(14.dp)
                            ),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricIndigo,
                            disabledContainerColor = Color(0x18FFFFFF)
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = if (selectedCount > 0) Color.White else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "OPTIMIZE ($selectedCount)",
                                color = if (selectedCount > 0) Color.White else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Right Content Area (TV 2-Column Grid)
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search Input
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (uiState.searchQuery.isEmpty()) {
                                Text(
                                    text = "Type or press voice remote to search apps...",
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
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Update Banner (if an update is ready or compiling on GitHub Actions)
                if (uiState.availableUpdate != null) {
                    UpdateBanner(
                        update = uiState.availableUpdate,
                        onDownloadClick = onDownloadUpdate,
                        onDismissClick = onDismissUpdate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }

                // 2-Column Grid of D-Pad Focusable Cards (Full names, NO overlap)
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ElectricIndigo)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(bottom = 70.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
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

                // Remote Navigation Legend
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "D-PAD ARROWS: NAVIGATE // CENTER (OK): TOGGLE // BUTTON: OPTIMIZE",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${uiState.filteredApps.size} APPS LISTED",
                        color = ElectricViolet,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // TV Mini Bar (Floating if compiling in background)
            if (uiState.isCompiling && !uiState.showConsoleDialog) {
                CompilationMiniBar(
                    currentApp = uiState.currentCompilingApp,
                    currentIndex = uiState.compilingIndex,
                    totalCount = uiState.compilingTotal,
                    onOpenConsole = onOpenConsole,
                    onCancel = onCancelOptimization,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 36.dp)
                )
            }
        }
    }

    // Shield TV Shizuku / Root Setup Guide Dialog
    if (showTvHelpDialog) {
        Dialog(onDismissRequest = { showTvHelpDialog = false }) {
            Box(
                modifier = Modifier
                    .width(520.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkMidnight)
                    .border(1.5.dp, TvFocusHighlight, RoundedCornerShape(20.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "NVIDIA SHIELD TV SETUP GUIDE",
                        color = ElectricViolet,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "DexOptX uses Shizuku or direct Root (su) to execute native dexopt commands without PC connection.",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "OPTION 1: IF SHIELD IS ROOTED\n" +
                                "• DexOptX will automatically use 'su' binary. Tap 'CONNECT SHIZUKU' and grant Superuser privilege.\n\n" +
                                "OPTION 2: WIRELESS DEBUGGING (NO ROOT)\n" +
                                "1. Install Downloader app on Shield TV and download Shizuku apk (or transfer via USB).\n" +
                                "2. Go to Shield Settings -> Device Preferences -> Developer Options.\n" +
                                "3. Turn ON 'Network Debugging' (Wireless Debugging).\n" +
                                "4. Open Shizuku on Shield and choose 'Start via Wireless Debugging'.\n" +
                                "5. Return to DexOptX and tap 'CONNECT SHIZUKU'.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    var isCloseFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = { showTvHelpDialog = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusable()
                            .onFocusChanged { isCloseFocused = it.isFocused }
                            .border(
                                width = if (isCloseFocused) 2.dp else 0.dp,
                                color = TvFocusHighlight,
                                shape = RoundedCornerShape(10.dp)
                            ),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                    ) {
                        Text("GOT IT // CLOSE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
