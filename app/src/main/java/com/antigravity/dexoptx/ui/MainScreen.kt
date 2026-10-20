package com.antigravity.dexoptx.ui

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antigravity.dexoptx.ui.components.AmbientMeshBackground
import com.antigravity.dexoptx.ui.components.AppDexoptInspectorDialog
import com.antigravity.dexoptx.ui.components.LiveConsoleDialog

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Automatically refresh installed apps and ART compilation state when returning to the app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Detect if running on Android TV or wide landscape
    val isTvDevice = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION) ||
            configuration.screenWidthDp >= 840

    Box(modifier = modifier.fillMaxSize()) {
        // Liquid Glass Ambient Mesh Backdrop
        AmbientMeshBackground()

        if (isTvDevice) {
            TvLayout(
                uiState = uiState,
                onSearchChange = viewModel::onSearchQueryChange,
                onCategoryChange = viewModel::onCategoryChange,
                onStatusFilterChange = viewModel::onStatusFilterChange,
                onFilterChange = viewModel::onFilterChange,
                onToggleApp = viewModel::onToggleAppSelection,
                onSelectAll = viewModel::onSelectAll,
                onDeselectAll = viewModel::onDeselectAll,
                onModeChange = viewModel::onCompileModeChange,
                onOptimizeClick = viewModel::startOptimization,
                onRequestShizuku = viewModel::requestShizukuPermission,
                onCheckUpdate = { viewModel.checkForUpdates(isManual = true) },
                onDownloadUpdate = viewModel::performDownloadUpdate,
                onDismissUpdate = viewModel::dismissUpdateBanner,
                onOpenConsole = viewModel::openConsoleDialog,
                onCancelOptimization = viewModel::cancelOptimization,
                onInspectApp = viewModel::inspectApp,
                onRefreshAll = viewModel::refreshAll
            )
        } else {
            PhoneLayout(
                uiState = uiState,
                onSearchChange = viewModel::onSearchQueryChange,
                onCategoryChange = viewModel::onCategoryChange,
                onStatusFilterChange = viewModel::onStatusFilterChange,
                onFilterChange = viewModel::onFilterChange,
                onToggleApp = viewModel::onToggleAppSelection,
                onSelectAll = viewModel::onSelectAll,
                onDeselectAll = viewModel::onDeselectAll,
                onModeChange = viewModel::onCompileModeChange,
                onOptimizeClick = viewModel::startOptimization,
                onRequestShizuku = viewModel::requestShizukuPermission,
                onCheckUpdate = { viewModel.checkForUpdates(isManual = true) },
                onDownloadUpdate = viewModel::performDownloadUpdate,
                onDismissUpdate = viewModel::dismissUpdateBanner,
                onOpenConsole = viewModel::openConsoleDialog,
                onCancelOptimization = viewModel::cancelOptimization,
                onInspectApp = viewModel::inspectApp,
                onRefreshAll = viewModel::refreshAll
            )
        }

        // Live Real-Time Console Progress Dialog
        if (uiState.showConsoleDialog) {
            LiveConsoleDialog(
                isRunning = uiState.isCompiling,
                currentApp = uiState.currentCompilingApp,
                currentIndex = uiState.compilingIndex,
                totalCount = uiState.compilingTotal,
                logs = uiState.consoleLogs,
                onDismiss = viewModel::closeConsoleDialog,
                onCancel = viewModel::cancelOptimization
            )
        }

        // ART Dexopt Inspector Modal Dialog
        uiState.inspectingApp?.let { app ->
            AppDexoptInspectorDialog(
                app = app,
                details = uiState.inspectingDetails,
                isLoading = uiState.isInspectingLoading,
                onRecheck = viewModel::recheckInspectingApp,
                onCompileSpeed = { viewModel.optimizeSingleApp(app.packageName, "speed") },
                onCompileProfile = { viewModel.optimizeSingleApp(app.packageName, "speed-profile") },
                onDismiss = viewModel::closeAppInspector
            )
        }
    }
}
