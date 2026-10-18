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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.antigravity.dexoptx.theme.DarkMidnight
import com.antigravity.dexoptx.theme.ElectricIndigo
import com.antigravity.dexoptx.theme.ElectricViolet
import com.antigravity.dexoptx.theme.GlassBorder
import com.antigravity.dexoptx.theme.GlassSurface
import com.antigravity.dexoptx.theme.NeonEmerald
import com.antigravity.dexoptx.theme.TextMuted
import com.antigravity.dexoptx.theme.TextPrimary
import com.antigravity.dexoptx.ui.components.AppCard
import com.antigravity.dexoptx.ui.components.CompilationMiniBar
import com.antigravity.dexoptx.ui.components.UpdateBanner

private val TvCyanFocusBorder = Color(0xFF38BDF8)
private val TvCyanFocusFill = Color(0x4038BDF8)

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

    val initialFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        try {
            initialFocusRequester.requestFocus()
        } catch (_: Throwable) {}
    }

    val catApps = when (uiState.activeCategory) {
        CategoryFilter.ALL -> uiState.allApps
        CategoryFilter.USER_ONLY -> uiState.allApps.filter { !it.isSystemApp }
        CategoryFilter.SYSTEM_ONLY -> uiState.allApps.filter { it.isSystemApp }
    }
    val totalInCat = catApps.size
    val compiledInCat = catApps.count { it.compileStatus.isCompiled }
    val uncompiledInCat = totalInCat - compiledInCat

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        // Left Control Panel: Vertically scrollable to guarantee all options are on-screen
        Box(
            modifier = Modifier
                .width(300.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xD90E1017))
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Header
                Text(
                    text = "DEXOPT-X TV",
                    color = ElectricViolet,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Nvidia Shield • v1.0.3 • ARM64",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Check for Updates Button
                var isCheckUpdateFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isCheckUpdateFocused) TvCyanFocusFill else Color(0x18FFFFFF))
                        .border(
                            width = if (isCheckUpdateFocused) 3.dp else 1.dp,
                            color = if (isCheckUpdateFocused) TvCyanFocusBorder else GlassBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .focusable()
                        .onFocusChanged { isCheckUpdateFocused = it.isFocused }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            onCheckUpdate()
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = if (isCheckUpdateFocused) TvCyanFocusBorder else TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isCheckingUpdate) "CHECKING FOR UPDATES..." else "CHECK FOR UPDATES",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Shizuku / Root Status Pill + TV Help Guide
                var isShizukuFocused by remember { mutableStateOf(false) }
                var isHelpFocused by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isShizukuFocused) TvCyanFocusFill
                                else (if (uiState.isShizukuConnected) NeonEmerald.copy(alpha = 0.15f) else Color(0x33EF4444))
                            )
                            .border(
                                width = if (isShizukuFocused) 3.dp else 1.dp,
                                color = if (isShizukuFocused) TvCyanFocusBorder else (if (uiState.isShizukuConnected) NeonEmerald.copy(alpha = 0.4f) else Color(0x66EF4444)),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .focusable()
                            .onFocusChanged { isShizukuFocused = it.isFocused }
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                onRequestShizuku()
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (uiState.isShizukuConnected) "ROOT/SHIZUKU ACTIVE" else "CONNECT SHIZUKU/ROOT",
                            color = if (isShizukuFocused) Color.White else (if (uiState.isShizukuConnected) NeonEmerald else Color(0xFFFCA5A5)),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isHelpFocused) TvCyanFocusFill else Color(0x22FFFFFF))
                            .border(
                                width = if (isHelpFocused) 3.dp else 1.dp,
                                color = if (isHelpFocused) TvCyanFocusBorder else GlassBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .focusable()
                            .onFocusChanged { isHelpFocused = it.isFocused }
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                showTvHelpDialog = true
                            }
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
                    CategoryFilter.values().forEachIndexed { index, cat ->
                        val isSelected = uiState.activeCategory == cat
                        var isCatFocused by remember { mutableStateOf(false) }
                        val count = when (cat) {
                            CategoryFilter.ALL -> uiState.allApps.size
                            CategoryFilter.USER_ONLY -> uiState.allApps.count { !it.isSystemApp }
                            CategoryFilter.SYSTEM_ONLY -> uiState.allApps.count { it.isSystemApp }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (index == 0) Modifier.focusRequester(initialFocusRequester) else Modifier)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        isCatFocused -> TvCyanFocusFill
                                        isSelected -> ElectricIndigo
                                        else -> Color(0x18FFFFFF)
                                    }
                                )
                                .border(
                                    width = if (isCatFocused) 3.dp else 1.dp,
                                    color = if (isCatFocused) TvCyanFocusBorder else (if (isSelected) ElectricViolet else GlassBorder),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .focusable()
                                .onFocusChanged { isCatFocused = it.isFocused }
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    onCategoryChange(cat)
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cat.label,
                                    color = if (isCatFocused || isSelected) Color.White else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "$count",
                                    color = if (isCatFocused || isSelected) Color.White else TextMuted,
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
                                .background(
                                    when {
                                        isChipFocused -> TvCyanFocusFill
                                        isStatSelected -> Color(0xFF334155)
                                        else -> Color(0x18FFFFFF)
                                    }
                                )
                                .border(
                                    width = if (isChipFocused) 3.dp else 1.dp,
                                    color = if (isChipFocused) TvCyanFocusBorder else (if (isStatSelected) Color(0xFF94A3B8) else GlassBorder),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .focusable()
                                .onFocusChanged { isChipFocused = it.isFocused }
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    onStatusFilterChange(sf)
                                }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = lbl,
                                    color = if (isChipFocused || isStatSelected) Color.White else TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "($count)",
                                    color = if (isChipFocused || isStatSelected) Color.White else TextMuted,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // D-Pad Remote Focusable Compile Profile (Fully visible on-screen)
                Text(
                    text = "COMPILE PROFILE",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "PROFILE" to "speed-profile",
                        "SPEED" to "speed"
                    ).forEach { (label, mode) ->
                        val isModeActive = uiState.compileMode == mode
                        var isModeFocused by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        isModeFocused -> TvCyanFocusFill
                                        isModeActive -> ElectricIndigo
                                        else -> Color(0x18FFFFFF)
                                    }
                                )
                                .border(
                                    width = if (isModeFocused) 3.dp else 1.dp,
                                    color = if (isModeFocused) TvCyanFocusBorder else (if (isModeActive) ElectricViolet else GlassBorder),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .focusable()
                                .onFocusChanged { isModeFocused = it.isFocused }
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    onModeChange(mode)
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isModeFocused || isModeActive) Color.White else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Select All & Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var isSelectAllFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelectAllFocused) TvCyanFocusFill else Color(0x22FFFFFF))
                            .border(
                                width = if (isSelectAllFocused) 3.dp else 1.dp,
                                color = if (isSelectAllFocused) TvCyanFocusBorder else GlassBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .focusable()
                            .onFocusChanged { isSelectAllFocused = it.isFocused }
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                onSelectAll()
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("SELECT ALL", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    var isClearFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isClearFocused) TvCyanFocusFill else Color(0x22FFFFFF))
                            .border(
                                width = if (isClearFocused) 3.dp else 1.dp,
                                color = if (isClearFocused) TvCyanFocusBorder else GlassBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .focusable()
                            .onFocusChanged { isClearFocused = it.isFocused }
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                onDeselectAll()
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("CLEAR", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Big Primary Optimize Button
                var isOptimizeFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            when {
                                selectedCount == 0 -> Color(0x18FFFFFF)
                                isOptimizeFocused -> Color(0xFF4F46E5)
                                else -> ElectricIndigo
                            }
                        )
                        .border(
                            width = if (isOptimizeFocused) 3.5.dp else 1.dp,
                            color = if (isOptimizeFocused) TvCyanFocusBorder else (if (selectedCount > 0) ElectricViolet else Color.Transparent),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .focusable(enabled = selectedCount > 0)
                        .onFocusChanged { isOptimizeFocused = it.isFocused }
                        .clickable(enabled = selectedCount > 0, interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            onOptimizeClick()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
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

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.width(20.dp))

        // Right Content Area (TV 2-Column Grid)
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search Input
                var isSearchFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSearchFocused) TvCyanFocusFill else GlassSurface)
                        .border(
                            width = if (isSearchFocused) 3.dp else 1.dp,
                            color = if (isSearchFocused) TvCyanFocusBorder else GlassBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 11.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = if (isSearchFocused) Color.White else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (uiState.searchQuery.isEmpty()) {
                                Text(
                                    text = "Search ${uiState.filteredApps.size} apps...",
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { isSearchFocused = it.isFocused }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Update Banner (if an update is ready or compiling on GitHub Actions)
                if (uiState.availableUpdate != null) {
                    UpdateBanner(
                        update = uiState.availableUpdate,
                        onDownloadClick = onDownloadUpdate,
                        onDismissClick = onDismissUpdate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
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
                        text = "D-PAD: NAVIGATE // OK: TOGGLE // CYAN BORDER = CURRENT FOCUS",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${uiState.filteredApps.size} APPS LISTED",
                        color = ElectricViolet,
                        fontSize = 10.sp,
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
                        .padding(bottom = 24.dp)
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
                    .border(2.dp, TvCyanFocusBorder, RoundedCornerShape(20.dp))
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
                        text = "DexOptX uses direct Root (su) or Shizuku to execute native dexopt commands directly on your Nvidia Shield TV.",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "OPTION 1: IF SHIELD IS ROOTED\n" +
                                "• DexOptX will automatically use the 'su' binary. Tap 'CONNECT SHIZUKU/ROOT' and grant Superuser privilege.\n\n" +
                                "OPTION 2: WIRELESS DEBUGGING (NO ROOT)\n" +
                                "1. Install Downloader app on Shield TV and download Shizuku apk (or transfer via USB).\n" +
                                "2. Go to Shield Settings -> Device Preferences -> Developer Options.\n" +
                                "3. Turn ON 'Network Debugging' (Wireless Debugging).\n" +
                                "4. Open Shizuku on Shield and choose 'Start via Wireless Debugging'.\n" +
                                "5. Return to DexOptX and tap 'CONNECT SHIZUKU/ROOT'.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    var isCloseFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCloseFocused) TvCyanFocusFill else ElectricIndigo)
                            .border(
                                width = if (isCloseFocused) 3.dp else 1.dp,
                                color = if (isCloseFocused) TvCyanFocusBorder else GlassBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .focusable()
                            .onFocusChanged { isCloseFocused = it.isFocused }
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                showTvHelpDialog = false
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("GOT IT // CLOSE", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
