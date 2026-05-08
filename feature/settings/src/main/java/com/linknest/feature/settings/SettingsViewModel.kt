package com.linknest.feature.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linknest.core.action.ActionResult
import com.linknest.core.action.pipeline.HealthCheckPipeline
import com.linknest.core.data.backup.BackupFileManager
import com.linknest.core.data.backup.BackupPackage
import com.linknest.core.data.backup.BackupSerializer
import com.linknest.core.data.backup.ReadResult
import com.linknest.core.data.backup.StagedBackupInfo
import com.linknest.core.data.backup.WriteResult
import com.linknest.core.data.usecase.ExportDataUseCase
import com.linknest.core.data.usecase.ImportDataUseCase
import com.linknest.core.data.usecase.ObserveUserPreferencesUseCase
import com.linknest.core.data.usecase.UpdateBackgroundHealthChecksUseCase
import com.linknest.core.data.usecase.UpdateBackupFolderUriUseCase
import com.linknest.core.data.usecase.UpdateTileDensityModeUseCase
import com.linknest.core.data.usecase.UpdateTileSizeUseCase
import com.linknest.core.model.HealthReportItem
import com.linknest.core.model.TileDensityMode
import com.linknest.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ExportStatus {
    object Idle : ExportStatus
    object Building : ExportStatus
    data class ReadyToSave(val pkg: BackupPackage) : ExportStatus
    data class Saved(val location: String) : ExportStatus
    data class Failed(val reason: String) : ExportStatus
}

sealed interface ImportStatus {
    object Idle : ImportStatus
    object Running : ImportStatus
    data class Success(val websiteCount: Int, val categoryCount: Int) : ImportStatus
    data class Failed(val reason: String) : ImportStatus
}

data class SettingsUiState(
    val isLoading: Boolean = true,
    val preferences: UserPreferences = UserPreferences(),
    val exportStatus: ExportStatus = ExportStatus.Idle,
    val importStatus: ImportStatus = ImportStatus.Idle,
    val stagedInfo: StagedBackupInfo? = null,
    val isRunningHealthCheck: Boolean = false,
    val latestHealthReport: List<HealthReportItem> = emptyList(),
    val healthSummary: String? = null,
    val userMessage: String? = null,
) {
    val isExporting: Boolean get() = exportStatus is ExportStatus.Building
    val isImporting: Boolean get() = importStatus is ImportStatus.Running
    val hasStagedBackup: Boolean get() = stagedInfo != null
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeUserPreferencesUseCase: ObserveUserPreferencesUseCase,
    private val updateTileSizeUseCase: UpdateTileSizeUseCase,
    private val updateTileDensityModeUseCase: UpdateTileDensityModeUseCase,
    private val updateBackgroundHealthChecksUseCase: UpdateBackgroundHealthChecksUseCase,
    private val updateBackupFolderUriUseCase: UpdateBackupFolderUriUseCase,
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase,
    private val backupSerializer: BackupSerializer,
    private val backupFileManager: BackupFileManager,
    private val healthCheckPipeline: HealthCheckPipeline,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeUserPreferencesUseCase().collect { prefs ->
                _uiState.update { it.copy(isLoading = false, preferences = prefs) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val info = backupFileManager.getStagedBackupInfo()
            _uiState.update { it.copy(stagedInfo = info) }
        }
    }

    // ── Preferences ──────────────────────────────────────────────────────────

    fun onTileDensityModeSelected(mode: TileDensityMode) = viewModelScope.launch { updateTileDensityModeUseCase(mode) }
    fun onTileSizeSelected(sizeDp: Int) = viewModelScope.launch { updateTileSizeUseCase(sizeDp) }
    fun onBackgroundHealthChecksChanged(enabled: Boolean) = viewModelScope.launch { updateBackgroundHealthChecksUseCase(enabled) }

    fun onBackupFolderSelected(uri: String?) {
        viewModelScope.launch {
            updateBackupFolderUriUseCase(uri)
            _uiState.update {
                it.copy(userMessage = if (uri != null) "Backup folder set." else "Backup folder cleared.")
            }
        }
    }

    // ── Export ───────────────────────────────────────────────────────────────

    fun onExportBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(exportStatus = ExportStatus.Building, userMessage = null) }
            runCatching {
                val snapshot = exportDataUseCase()
                val pkg = backupSerializer.serialize(snapshot)
                val staged = backupFileManager.stageBackup(pkg)
                val info = backupFileManager.getStagedBackupInfo()
                _uiState.update {
                    it.copy(
                        exportStatus = ExportStatus.ReadyToSave(pkg),
                        stagedInfo = info,
                        userMessage = "Backup ready. Choose where to save.",
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        exportStatus = ExportStatus.Failed(e.message ?: "Export failed."),
                        userMessage = e.message ?: "Export failed.",
                    )
                }
            }
        }
    }

    fun onSaveExportToUri(resolver: ContentResolver, uri: Uri) {
        val pkg = (uiState.value.exportStatus as? ExportStatus.ReadyToSave)?.pkg ?: return
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = backupFileManager.writeToUri(resolver, uri, pkg)) {
                is WriteResult.Ok -> _uiState.update {
                    it.copy(
                        exportStatus = ExportStatus.Saved(result.uriLabel),
                        userMessage = "Backup saved successfully.",
                    )
                }
                is WriteResult.Error -> _uiState.update {
                    it.copy(
                        exportStatus = ExportStatus.Failed(result.message),
                        userMessage = result.message,
                    )
                }
            }
        }
    }

    fun onExportSaveCancelled() {
        _uiState.update {
            it.copy(
                exportStatus = ExportStatus.Idle,
                userMessage = "Save cancelled. Backup is staged — click \"Import last export\" to restore.",
            )
        }
    }

    fun resetExportStatus() {
        _uiState.update { it.copy(exportStatus = ExportStatus.Idle) }
    }

    // ── Import ───────────────────────────────────────────────────────────────

    fun onImportFromStaged() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(importStatus = ImportStatus.Running, userMessage = null) }
            when (val read = backupFileManager.readStagedBackup()) {
                is ReadResult.Ok -> performImport(read.payload)
                is ReadResult.Error -> _uiState.update {
                    it.copy(importStatus = ImportStatus.Failed(read.message), userMessage = read.message)
                }
            }
        }
    }

    fun onImportFromUri(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(importStatus = ImportStatus.Running, userMessage = null) }
            when (val read = backupFileManager.readFromUri(resolver, uri)) {
                is ReadResult.Ok -> performImport(read.payload)
                is ReadResult.Error -> _uiState.update {
                    it.copy(importStatus = ImportStatus.Failed(read.message), userMessage = read.message)
                }
            }
        }
    }

    fun onImportFromText(text: String) {
        if (text.isBlank()) {
            _uiState.update { it.copy(userMessage = "Backup text is empty.") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(importStatus = ImportStatus.Running, userMessage = null) }
            performImport(text)
        }
    }

    private suspend fun performImport(payload: String) {
        runCatching {
            val snapshot = backupSerializer.deserialize(payload)
            importDataUseCase(snapshot)
        }.onSuccess { summary ->
            _uiState.update {
                it.copy(
                    importStatus = ImportStatus.Success(
                        websiteCount = summary.importedWebsites,
                        categoryCount = summary.importedCategories,
                    ),
                    userMessage = "Restored ${summary.importedWebsites} websites across ${summary.importedCategories} categories.",
                )
            }
        }.onFailure { e ->
            val msg = e.message ?: "Import failed."
            _uiState.update {
                it.copy(importStatus = ImportStatus.Failed(msg), userMessage = msg)
            }
        }
    }

    fun resetImportStatus() {
        _uiState.update { it.copy(importStatus = ImportStatus.Idle) }
    }

    // ── Health check ─────────────────────────────────────────────────────────

    fun onRunHealthCheck() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRunningHealthCheck = true, userMessage = null) }
            val staleBefore = System.currentTimeMillis() - (12L * 60L * 60L * 1000L)
            when (val result = healthCheckPipeline(staleBefore = staleBefore)) {
                is ActionResult.Success -> _uiState.update {
                    it.copy(
                        isRunningHealthCheck = false,
                        healthSummary = "Checked ${result.value.checkedCount} links — OK ${result.value.okCount}, issues ${result.value.deadCount + result.value.timeoutCount + result.value.blockedCount}.",
                        latestHealthReport = result.value.items,
                    )
                }
                is ActionResult.PartialSuccess -> _uiState.update {
                    it.copy(
                        isRunningHealthCheck = false,
                        healthSummary = "Checked ${result.value.checkedCount} links with warnings.",
                        latestHealthReport = result.value.items,
                        userMessage = result.issues.firstOrNull()?.message,
                    )
                }
                is ActionResult.Failure -> _uiState.update {
                    it.copy(isRunningHealthCheck = false, userMessage = result.issue.message)
                }
            }
        }
    }

    fun onMessageConsumed() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
