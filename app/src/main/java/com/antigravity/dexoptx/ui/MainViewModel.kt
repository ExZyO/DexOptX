package com.antigravity.dexoptx.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.dexoptx.data.AppRepository
import com.antigravity.dexoptx.model.AppInfo
import com.antigravity.dexoptx.model.CompileStatus
import com.antigravity.dexoptx.shizuku.ShizukuController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class CategoryFilter(val label: String) {
    USER_ONLY("USER APPS"),
    SYSTEM_ONLY("SYSTEM"),
    ALL("ALL APPS")
}

enum class StatusFilter(val label: String) {
    ALL("ALL STATUS"),
    UNCOMPILED("UNCOMPILED"),
    COMPILED("COMPILED"),
    RECENTLY_UPDATED("RECENT UPDATES")
}

// Kept for backward compatibility
enum class AppFilter(val label: String) {
    ALL("ALL"),
    USER_ONLY("USER APPS"),
    SYSTEM_ONLY("SYSTEM"),
    RECENTLY_UPDATED("RECENT UPDATES")
}

data class MainUiState(
    val isLoading: Boolean = true,
    val allApps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val activeCategory: CategoryFilter = CategoryFilter.USER_ONLY,
    val activeStatusFilter: StatusFilter = StatusFilter.ALL,
    val activeFilter: AppFilter = AppFilter.USER_ONLY,
    val compileMode: String = "speed-profile",
    val isShizukuConnected: Boolean = false,
    val isCompiling: Boolean = false,
    val currentCompilingApp: String = "",
    val compilingIndex: Int = 0,
    val compilingTotal: Int = 0,
    val consoleLogs: List<String> = emptyList(),
    val showConsoleDialog: Boolean = false,
    val availableUpdate: com.antigravity.dexoptx.updater.UpdateInfo? = null,
    val isCheckingUpdate: Boolean = false,
    val isDownloadingUpdate: Boolean = false,
    val updateDownloadProgress: Float = 0f,
    val updateStatusMessage: String? = null,
    val inspectingApp: AppInfo? = null,
    val inspectingDetails: com.antigravity.dexoptx.shizuku.DexoptDetails? = null,
    val isInspectingLoading: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    val shizukuController = ShizukuController(application)
    val updateManager = com.antigravity.dexoptx.updater.UpdateManager(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val packageReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            val action = intent?.action ?: return
            if (action == android.content.Intent.ACTION_PACKAGE_ADDED ||
                action == android.content.Intent.ACTION_PACKAGE_REPLACED ||
                action == android.content.Intent.ACTION_PACKAGE_REMOVED
            ) {
                refreshAll()
            }
        }
    }

    init {
        val filter = android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_PACKAGE_ADDED)
            addAction(android.content.Intent.ACTION_PACKAGE_REPLACED)
            addAction(android.content.Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        try {
            application.registerReceiver(packageReceiver, filter)
        } catch (_: Throwable) {}

        refreshAll()
        checkForUpdates(isManual = false)
    }

    fun refreshAll() {
        refreshShizukuStatus()
        loadInstalledApps()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(packageReceiver)
        } catch (_: Throwable) {}
    }

    private var updatePollJob: kotlinx.coroutines.Job? = null

    fun checkForUpdates(isManual: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdate = true, updateStatusMessage = null) }
            val result = updateManager.checkForUpdates()
            result.onSuccess { update ->
                if (update.isNewer) {
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            availableUpdate = update,
                            updateStatusMessage = if (update.isBuilding) {
                                "Compiling on GitHub: ${update.latestVersion}"
                            } else {
                                "Update ready: ${update.latestVersion}"
                            }
                        )
                    }
                    if (update.isBuilding) {
                        startUpdatePolling()
                    } else {
                        stopUpdatePolling()
                    }
                } else {
                    stopUpdatePolling()
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            availableUpdate = null,
                            updateStatusMessage = if (isManual) "You are on the latest version (${update.currentVersion})" else null
                        )
                    }
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isCheckingUpdate = false,
                        updateStatusMessage = if (isManual) "Update check failed: ${err.localizedMessage}" else null
                    )
                }
            }
        }
    }

    private fun startUpdatePolling() {
        updatePollJob?.cancel()
        updatePollJob = viewModelScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(15000)
                updateManager.checkForUpdates().onSuccess { update ->
                    if (update.isNewer) {
                        _uiState.update { it.copy(availableUpdate = update) }
                        if (!update.isBuilding) {
                            _uiState.update { it.copy(updateStatusMessage = "Update ready: ${update.latestVersion}") }
                            return@launch
                        }
                    } else {
                        _uiState.update { it.copy(availableUpdate = null) }
                        return@launch
                    }
                }
            }
        }
    }

    private fun stopUpdatePolling() {
        updatePollJob?.cancel()
        updatePollJob = null
    }

    fun dismissUpdateBanner() {
        stopUpdatePolling()
        _uiState.update { it.copy(availableUpdate = null, updateStatusMessage = null) }
    }

    fun performDownloadUpdate() {
        val update = _uiState.value.availableUpdate ?: return
        if (update.isBuilding) {
            updateManager.openDownloadUrl(update.actionsUrl)
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDownloadingUpdate = true,
                    updateDownloadProgress = 0.05f,
                    updateStatusMessage = "Connecting to download ${update.latestVersion}..."
                )
            }

            val downloadResult = updateManager.downloadApkDirectly(update.apkUrl) { bytesRead, totalBytes ->
                if (totalBytes > 0) {
                    val progress = (bytesRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                    val percent = (progress * 100).toInt()
                    val mbRead = String.format(java.util.Locale.US, "%.1f", bytesRead / (1024f * 1024f))
                    val mbTotal = String.format(java.util.Locale.US, "%.1f", totalBytes / (1024f * 1024f))
                    _uiState.update {
                        it.copy(
                            updateDownloadProgress = progress,
                            updateStatusMessage = "Downloading: $percent% ($mbRead MB / $mbTotal MB)"
                        )
                    }
                }
            }

            downloadResult.onSuccess { apkFile ->
                _uiState.update {
                    it.copy(
                        isDownloadingUpdate = false,
                        updateDownloadProgress = 1f,
                        updateStatusMessage = "Installing ${update.latestVersion}..."
                    )
                }
                updateManager.installApk(apkFile, shizukuController)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isDownloadingUpdate = false,
                        updateStatusMessage = "Direct download failed: ${err.localizedMessage}. Opening browser..."
                    )
                }
                updateManager.openDownloadUrl(update.apkUrl)
            }
        }
    }

    fun refreshShizukuStatus() {
        val isReady = shizukuController.isExecutionReady()
        _uiState.update { it.copy(isShizukuConnected = isReady) }
        if (isReady) {
            refreshCompilationStatuses()
        }
    }

    fun requestShizukuPermission() {
        if (shizukuController.isRootAvailable()) {
            _uiState.update { it.copy(isShizukuConnected = true) }
            refreshCompilationStatuses()
            return
        }
        shizukuController.requestPermission { granted ->
            val isReady = granted || shizukuController.isRootAvailable()
            _uiState.update { it.copy(isShizukuConnected = isReady) }
            if (isReady) {
                refreshCompilationStatuses()
            }
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val apps = repository.loadInstalledApps()
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    allApps = apps,
                    filteredApps = applyFilters(apps, state.searchQuery, state.activeCategory, state.activeStatusFilter)
                )
            }
            if (_uiState.value.isShizukuConnected) {
                refreshCompilationStatuses()
            }
        }
    }

    fun refreshCompilationStatuses() {
        if (!_uiState.value.isShizukuConnected) return
        viewModelScope.launch {
            try {
                val rawLines = mutableListOf<String>()
                shizukuController.executeCommand(arrayOf("dumpsys", "package", "dexopt")).collect { line ->
                    rawLines.add(line)
                }

                val detailsMap = shizukuController.parseDexoptDump(rawLines)
                if (detailsMap.isNotEmpty()) {
                    _uiState.update { state ->
                        val updated = state.allApps.map { app ->
                            val details = detailsMap[app.packageName]
                            if (details != null) {
                                app.copy(
                                    compileStatus = details.status,
                                    compilationFilter = details.filter,
                                    compilationReason = details.reason,
                                    primaryAbi = details.primaryAbi
                                )
                            } else {
                                app
                            }
                        }
                        state.copy(
                            allApps = updated,
                            filteredApps = applyFilters(updated, state.searchQuery, state.activeCategory, state.activeStatusFilter)
                        )
                    }
                }
            } catch (_: Exception) {
                // Ignore parse failures
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredApps = applyFilters(state.allApps, query, state.activeCategory, state.activeStatusFilter)
            )
        }
    }

    fun onCategoryChange(category: CategoryFilter) {
        _uiState.update { state ->
            state.copy(
                activeCategory = category,
                filteredApps = applyFilters(state.allApps, state.searchQuery, category, state.activeStatusFilter)
            )
        }
    }

    fun onStatusFilterChange(status: StatusFilter) {
        _uiState.update { state ->
            state.copy(
                activeStatusFilter = status,
                filteredApps = applyFilters(state.allApps, state.searchQuery, state.activeCategory, status)
            )
        }
    }

    // Kept for backward compatibility
    fun onFilterChange(filter: AppFilter) {
        val cat = when (filter) {
            AppFilter.USER_ONLY -> CategoryFilter.USER_ONLY
            AppFilter.SYSTEM_ONLY -> CategoryFilter.SYSTEM_ONLY
            AppFilter.ALL -> CategoryFilter.ALL
            AppFilter.RECENTLY_UPDATED -> CategoryFilter.ALL
        }
        val stat = if (filter == AppFilter.RECENTLY_UPDATED) StatusFilter.RECENTLY_UPDATED else StatusFilter.ALL
        _uiState.update { state ->
            state.copy(
                activeCategory = cat,
                activeStatusFilter = stat,
                activeFilter = filter,
                filteredApps = applyFilters(state.allApps, state.searchQuery, cat, stat)
            )
        }
    }

    fun onToggleAppSelection(packageName: String) {
        _uiState.update { state ->
            val updatedAll = state.allApps.map {
                if (it.packageName == packageName) it.copy(isSelected = !it.isSelected) else it
            }
            state.copy(
                allApps = updatedAll,
                filteredApps = applyFilters(updatedAll, state.searchQuery, state.activeCategory, state.activeStatusFilter)
            )
        }
    }

    fun onSelectAll() {
        _uiState.update { state ->
            val visiblePackageNames = state.filteredApps.map { it.packageName }.toSet()
            val updatedAll = state.allApps.map {
                if (it.packageName in visiblePackageNames) it.copy(isSelected = true) else it
            }
            state.copy(
                allApps = updatedAll,
                filteredApps = applyFilters(updatedAll, state.searchQuery, state.activeCategory, state.activeStatusFilter)
            )
        }
    }

    fun onDeselectAll() {
        _uiState.update { state ->
            val visiblePackageNames = state.filteredApps.map { it.packageName }.toSet()
            val updatedAll = state.allApps.map {
                if (it.packageName in visiblePackageNames) it.copy(isSelected = false) else it
            }
            state.copy(
                allApps = updatedAll,
                filteredApps = applyFilters(updatedAll, state.searchQuery, state.activeCategory, state.activeStatusFilter)
            )
        }
    }

    fun onCompileModeChange(mode: String) {
        _uiState.update { it.copy(compileMode = mode) }
    }

    fun openConsoleDialog() {
        _uiState.update { it.copy(showConsoleDialog = true) }
    }

    fun closeConsoleDialog() {
        _uiState.update { it.copy(showConsoleDialog = false) }
    }

    private var compilationJob: kotlinx.coroutines.Job? = null

    fun cancelOptimization() {
        compilationJob?.cancel()
        compilationJob = null
        _uiState.update {
            it.copy(
                isCompiling = false,
                consoleLogs = it.consoleLogs + "[STOPPED] Compilation cancelled by user."
            )
        }
    }

    fun startOptimization() {
        // Strictly compile only the selected apps within the active filter
        val selected = _uiState.value.filteredApps.filter { it.isSelected }
        if (selected.isEmpty()) return

        val mode = _uiState.value.compileMode
        _uiState.update {
            it.copy(
                isCompiling = true,
                showConsoleDialog = true,
                compilingIndex = 0,
                compilingTotal = selected.size,
                consoleLogs = listOf("Starting ART compilation for ${selected.size} apps [mode: $mode]...")
            )
        }

        compilationJob?.cancel()
        compilationJob = viewModelScope.launch {
            for ((index, app) in selected.withIndex()) {
                if (!isActive) break

                _uiState.update {
                    it.copy(
                        currentCompilingApp = app.appName,
                        compilingIndex = index + 1,
                        consoleLogs = it.consoleLogs + "[${index + 1}/${selected.size}] Compiling ${app.appName} (${app.packageName})..."
                    )
                }

                shizukuController.executeCommand(
                    arrayOf("cmd", "package", "compile", "-m", mode, "-f", app.packageName)
                ).collect { line ->
                    if (!this@launch.isActive) return@collect
                    _uiState.update { it.copy(consoleLogs = it.consoleLogs + line) }
                }

                // Query ground-truth ART compilation status after compilation completes
                val verifiedDetails = shizukuController.getAppDexoptDetails(app.packageName)
                if (this@launch.isActive) {
                    val verifyLog = if (verifiedDetails.status.isCompiled) {
                        "[VERIFIED] ART Status: ${verifiedDetails.status.label} (filter: ${verifiedDetails.filter}, reason: ${verifiedDetails.reason ?: "cmdline"})"
                    } else {
                        "[WARNING] ART Status: ${verifiedDetails.filter.uppercase()} - AOT machine code was not generated."
                    }
                    _uiState.update { state ->
                        val updated = state.allApps.map {
                            if (it.packageName == app.packageName) {
                                it.copy(
                                    compileStatus = verifiedDetails.status,
                                    compilationFilter = verifiedDetails.filter,
                                    compilationReason = verifiedDetails.reason,
                                    primaryAbi = verifiedDetails.primaryAbi
                                )
                            } else it
                        }
                        state.copy(
                            allApps = updated,
                            filteredApps = applyFilters(updated, state.searchQuery, state.activeCategory, state.activeStatusFilter),
                            consoleLogs = state.consoleLogs + verifyLog
                        )
                    }
                }
            }

            if (isActive) {
                _uiState.update {
                    it.copy(
                        isCompiling = false,
                        consoleLogs = it.consoleLogs + "[COMPLETE] All ${selected.size} apps processed."
                    )
                }
            }
        }
    }

    fun inspectApp(app: AppInfo) {
        _uiState.update {
            it.copy(
                inspectingApp = app,
                inspectingDetails = null,
                isInspectingLoading = true
            )
        }
        viewModelScope.launch {
            val details = shizukuController.getAppDexoptDetails(app.packageName)
            _uiState.update { state ->
                val updated = state.allApps.map {
                    if (it.packageName == app.packageName) {
                        it.copy(
                            compileStatus = details.status,
                            compilationFilter = details.filter,
                            compilationReason = details.reason,
                            primaryAbi = details.primaryAbi
                        )
                    } else it
                }
                state.copy(
                    inspectingDetails = details,
                    isInspectingLoading = false,
                    allApps = updated,
                    filteredApps = applyFilters(updated, state.searchQuery, state.activeCategory, state.activeStatusFilter)
                )
            }
        }
    }

    fun recheckInspectingApp() {
        val app = _uiState.value.inspectingApp ?: return
        inspectApp(app)
    }

    fun closeAppInspector() {
        _uiState.update { it.copy(inspectingApp = null, inspectingDetails = null, isInspectingLoading = false) }
    }

    fun optimizeSingleApp(packageName: String, mode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isInspectingLoading = true) }
            shizukuController.executeCommand(
                arrayOf("cmd", "package", "compile", "-m", mode, "-f", packageName)
            ).collect { _ -> }

            val details = shizukuController.getAppDexoptDetails(packageName)
            _uiState.update { state ->
                val updated = state.allApps.map {
                    if (it.packageName == packageName) {
                        it.copy(
                            compileStatus = details.status,
                            compilationFilter = details.filter,
                            compilationReason = details.reason,
                            primaryAbi = details.primaryAbi
                        )
                    } else it
                }
                state.copy(
                    inspectingDetails = details,
                    isInspectingLoading = false,
                    allApps = updated,
                    filteredApps = applyFilters(updated, state.searchQuery, state.activeCategory, state.activeStatusFilter)
                )
            }
        }
    }

    private fun applyFilters(
        apps: List<AppInfo>,
        query: String,
        category: CategoryFilter,
        status: StatusFilter
    ): List<AppInfo> {
        val now = System.currentTimeMillis()
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000

        return apps.filter { app ->
            val matchesCategory = when (category) {
                CategoryFilter.ALL -> true
                CategoryFilter.USER_ONLY -> !app.isSystemApp
                CategoryFilter.SYSTEM_ONLY -> app.isSystemApp
            }

            val matchesStatus = when (status) {
                StatusFilter.ALL -> true
                StatusFilter.UNCOMPILED -> !app.compileStatus.isCompiled
                StatusFilter.COMPILED -> app.compileStatus.isCompiled
                StatusFilter.RECENTLY_UPDATED -> (now - app.lastUpdateTime) < sevenDaysMs
            }

            val matchesQuery = query.isBlank() ||
                    app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)

            matchesCategory && matchesStatus && matchesQuery
        }
    }
}
