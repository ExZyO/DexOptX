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
import kotlinx.coroutines.launch

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
    val activeFilter: AppFilter = AppFilter.USER_ONLY,
    val compileMode: String = "speed-profile",
    val isShizukuConnected: Boolean = false,
    val isCompiling: Boolean = false,
    val currentCompilingApp: String = "",
    val compilingIndex: Int = 0,
    val compilingTotal: Int = 0,
    val consoleLogs: List<String> = emptyList(),
    val showConsoleDialog: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    val shizukuController = ShizukuController(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        refreshShizukuStatus()
        loadInstalledApps()
    }

    fun refreshShizukuStatus() {
        val available = shizukuController.isShizukuAvailable()
        val granted = shizukuController.isPermissionGranted()
        _uiState.update { it.copy(isShizukuConnected = available && granted) }
    }

    fun requestShizukuPermission() {
        shizukuController.requestPermission { granted ->
            _uiState.update { it.copy(isShizukuConnected = granted) }
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
                    filteredApps = applyFilters(apps, state.searchQuery, state.activeFilter)
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredApps = applyFilters(state.allApps, query, state.activeFilter)
            )
        }
    }

    fun onFilterChange(filter: AppFilter) {
        _uiState.update { state ->
            state.copy(
                activeFilter = filter,
                filteredApps = applyFilters(state.allApps, state.searchQuery, filter)
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
                filteredApps = applyFilters(updatedAll, state.searchQuery, state.activeFilter)
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
                filteredApps = applyFilters(updatedAll, state.searchQuery, state.activeFilter)
            )
        }
    }

    fun onDeselectAll() {
        _uiState.update { state ->
            val updatedAll = state.allApps.map { it.copy(isSelected = false) }
            state.copy(
                allApps = updatedAll,
                filteredApps = applyFilters(updatedAll, state.searchQuery, state.activeFilter)
            )
        }
    }

    fun onCompileModeChange(mode: String) {
        _uiState.update { it.copy(compileMode = mode) }
    }

    fun closeConsoleDialog() {
        _uiState.update { it.copy(showConsoleDialog = false) }
    }

    fun startOptimization() {
        val selected = _uiState.value.allApps.filter { it.isSelected }
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

        viewModelScope.launch {
            selected.forEachIndexed { index, app ->
                _uiState.update {
                    it.copy(
                        currentCompilingApp = app.appName,
                        compilingIndex = index + 1,
                        consoleLogs = it.consoleLogs + "[${index + 1}/${selected.size}] Compiling ${app.appName} (${app.packageName})..."
                    )
                }

                shizukuController.executeCommand(
                    arrayOf("cmd", "package", "compile", "-m", mode, app.packageName)
                ).collect { line ->
                    _uiState.update { it.copy(consoleLogs = it.consoleLogs + line) }
                }
            }

            // Trigger system-wide background dexopt pass to sweep containers
            _uiState.update { it.copy(consoleLogs = it.consoleLogs + "Running system maintenance sweep (cmd package bg-dexopt-job)...") }
            shizukuController.executeCommand(arrayOf("cmd", "package", "bg-dexopt-job")).collect { line ->
                _uiState.update { it.copy(consoleLogs = it.consoleLogs + line) }
            }

            _uiState.update {
                it.copy(
                    isCompiling = false,
                    consoleLogs = it.consoleLogs + "Optimization pass completed successfully!"
                )
            }
        }
    }

    private fun applyFilters(apps: List<AppInfo>, query: String, filter: AppFilter): List<AppInfo> {
        val now = System.currentTimeMillis()
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000

        return apps.filter { app ->
            val matchesFilter = when (filter) {
                AppFilter.ALL -> true
                AppFilter.USER_ONLY -> !app.isSystemApp
                AppFilter.SYSTEM_ONLY -> app.isSystemApp
                AppFilter.RECENTLY_UPDATED -> (now - app.lastUpdateTime) < sevenDaysMs
            }

            val matchesQuery = query.isBlank() ||
                    app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)

            matchesFilter && matchesQuery
        }
    }
}
