package com.antigravity.dexoptx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.antigravity.dexoptx.AppVersion
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

val TvCyanFocusBorder = Color(0xFF38BDF8)
val TvCyanFocusFill = Color(0x4038BDF8)

/**
 * Standard Android TV focusable interactive container.
 * Automatically highlights with an electric cyan border upon D-pad hover,
 * and triggers onClick on the very first remote press.
 */
@Composable
fun TvInteractiveBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    selectedColor: Color = ElectricIndigo,
    selectedBorderColor: Color = ElectricViolet,
    shape: RoundedCornerShape = RoundedCornerShape(10.dp),
    focusBorderWidth: Dp = 3.5.dp,
    unfocusedBorderWidth: Dp = 1.dp,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    content: @Composable (isFocused: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderWidth = if (isFocused) focusBorderWidth else unfocusedBorderWidth
    val borderColor = when {
        isFocused -> TvCyanFocusBorder
        isSelected -> selectedBorderColor
        else -> GlassBorder
    }
    val backgroundColor = when {
        isFocused -> TvCyanFocusFill
        isSelected -> selectedColor
        else -> Color(0x18FFFFFF)
    }

    Box(
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clip(shape)
            .background(backgroundColor)
            .border(width = borderWidth, color = borderColor, shape = shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
            }
    ) {
        content(isFocused)
    }
}

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
    onInspectApp: (com.antigravity.dexoptx.model.AppInfo) -> Unit = {},
    onRefreshAll: () -> Unit = {},
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
                    text = AppVersion.TV_LABEL,
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Check for Updates Button
                TvInteractiveBox(
                    onClick = onCheckUpdate,
                    modifier = Modifier.fillMaxWidth()
                ) { isFocused ->
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = if (isFocused) TvCyanFocusBorder else TextPrimary,
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

                Spacer(modifier = Modifier.height(6.dp))

                // Refresh Apps and ART Status Button
                TvInteractiveBox(
                    onClick = onRefreshAll,
                    modifier = Modifier.fillMaxWidth()
                ) { isFocused ->
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = if (isFocused) TvCyanFocusBorder else TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REFRESH APPS & ART STATE",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Shizuku / Root Status Pill + TV Help Guide
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TvInteractiveBox(
                        onClick = onRequestShizuku,
                        modifier = Modifier.weight(1f),
                        isSelected = uiState.isShizukuConnected,
                        selectedColor = NeonEmerald.copy(alpha = 0.15f),
                        selectedBorderColor = NeonEmerald.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    ) { isFocused ->
                        Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Text(
                                text = if (uiState.isShizukuConnected) "ROOT/SHIZUKU ACTIVE" else "CONNECT SHIZUKU/ROOT",
                                color = if (isFocused) Color.White else (if (uiState.isShizukuConnected) NeonEmerald else Color(0xFFFCA5A5)),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    TvInteractiveBox(
                        onClick = { showTvHelpDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(8.dp),
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
                        val isCatSelected = uiState.activeCategory == cat
                        val count = when (cat) {
                            CategoryFilter.ALL -> uiState.allApps.size
                            CategoryFilter.USER_ONLY -> uiState.allApps.count { !it.isSystemApp }
                            CategoryFilter.SYSTEM_ONLY -> uiState.allApps.count { it.isSystemApp }
                        }

                        TvInteractiveBox(
                            onClick = { onCategoryChange(cat) },
                            modifier = Modifier.fillMaxWidth(),
                            isSelected = isCatSelected,
                            selectedColor = ElectricIndigo,
                            selectedBorderColor = ElectricViolet,
                            focusRequester = if (index == 0) initialFocusRequester else null
                        ) { isFocused ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cat.label,
                                    color = if (isFocused || isCatSelected) Color.White else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "$count",
                                    color = if (isFocused || isCatSelected) Color.White else TextMuted,
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

                        TvInteractiveBox(
                            onClick = { onStatusFilterChange(sf) },
                            modifier = Modifier.weight(1f),
                            isSelected = isStatSelected,
                            selectedColor = Color(0xFF334155),
                            selectedBorderColor = Color(0xFF94A3B8),
                            shape = RoundedCornerShape(8.dp)
                        ) { isFocused ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = lbl,
                                    color = if (isFocused || isStatSelected) Color.White else TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "($count)",
                                    color = if (isFocused || isStatSelected) Color.White else TextMuted,
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

                        TvInteractiveBox(
                            onClick = { onModeChange(mode) },
                            modifier = Modifier.weight(1f),
                            isSelected = isModeActive,
                            selectedColor = ElectricIndigo,
                            selectedBorderColor = ElectricViolet
                        ) { isFocused ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isFocused || isModeActive) Color.White else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Select All & Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TvInteractiveBox(
                        onClick = onSelectAll,
                        modifier = Modifier.weight(1f),
                        selectedColor = Color(0x22FFFFFF)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("SELECT ALL", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }

                    TvInteractiveBox(
                        onClick = onDeselectAll,
                        modifier = Modifier.weight(1f),
                        selectedColor = Color(0x22FFFFFF)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("CLEAR", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Big Primary Optimize Button
                TvInteractiveBox(
                    onClick = onOptimizeClick,
                    enabled = selectedCount > 0,
                    modifier = Modifier.fillMaxWidth(),
                    isSelected = selectedCount > 0,
                    selectedColor = ElectricIndigo,
                    selectedBorderColor = ElectricViolet,
                    shape = RoundedCornerShape(14.dp),
                    focusBorderWidth = 4.dp
                ) { isFocused ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
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
                val searchInteractionSource = remember { MutableInteractionSource() }
                val isSearchFocused by searchInteractionSource.collectIsFocusedAsState()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSearchFocused) TvCyanFocusFill else GlassSurface)
                        .border(
                            width = if (isSearchFocused) 3.5.dp else 1.dp,
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
                                interactionSource = searchInteractionSource,
                                modifier = Modifier.fillMaxWidth()
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
                        isDownloading = uiState.isDownloadingUpdate,
                        downloadProgress = uiState.updateDownloadProgress,
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
                                onInspectApp = { onInspectApp(app) },
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
                    TvInteractiveBox(
                        onClick = { showTvHelpDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        isSelected = true,
                        selectedColor = ElectricIndigo,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
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
}
