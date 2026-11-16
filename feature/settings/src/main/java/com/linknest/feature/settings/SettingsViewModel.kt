package com.linknest.feature.settings

import androidx.core.net.toUri
import android.content.ContentResolver
import android.content.Context
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
import com.linknest.core.model.HealthReportItem
import com.linknest.core.model.TileDensityMode
import com.linknest.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    data class Preview(val snapshot: com.linknest.core.data.model.BackupSnapshot, val currentCount: Int) : ImportStatus
    data class Success(val websiteCount: Int, val categoryCount: Int) : ImportStatus
    data class Failed(val reason: String) : ImportStatus
}

data class SettingsUiState(
    val archivedCategories: List<com.linknest.core.model.SelectableCategory> = emptyList(),
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
    private val categoryRepository: com.linknest.core.data.repository.CategoryRepository,
    observeUserPreferencesUseCase: ObserveUserPreferencesUseCase,
    private val updateTileDensityModeUseCase: UpdateTileDensityModeUseCase,
    private val updateBackgroundHealthChecksUseCase: UpdateBackgroundHealthChecksUseCase,
    private val updateBackupFolderUriUseCase: UpdateBackupFolderUriUseCase,
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase,
    private val backupSerializer: BackupSerializer,
    private val backupFileManager: BackupFileManager,
    private val healthCheckPipeline: HealthCheckPipeline,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.observeArchivedCategories().collect { categories ->
                _uiState.update { it.copy(archivedCategories = categories) }
            }
        }

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
                backupFileManager.stageBackup(pkg)
                val info = backupFileManager.getStagedBackupInfo()
                val folderUri = uiState.value.preferences.backupFolderUri
                if (folderUri != null) {
                    // Auto-save to the user's chosen folder; fall back to the manual picker on failure.
                    when (val result = backupFileManager.writeToFolder(appContext.contentResolver, folderUri.toUri(), pkg)) {
                        is WriteResult.Ok -> _uiState.update {
                            it.copy(exportStatus = ExportStatus.Saved(result.uriLabel), stagedInfo = info, userMessage = "Backup saved to your chosen folder.")
                        }
                        is WriteResult.Error -> _uiState.update {
                            it.copy(exportStatus = ExportStatus.ReadyToSave(pkg), stagedInfo = info, userMessage = "${result.message} Choose where to save.")
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(exportStatus = ExportStatus.ReadyToSave(pkg), stagedInfo = info, userMessage = "Backup ready. Choose where to save.")
                    }
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
        val pkg = (uiState.value.exportStatus as? ExportStatus.ReadyToSave)?.pkg ?: run {
            // The staged package was lost (e.g. process death while the picker was open).
            _uiState.update {
                it.copy(
                    exportStatus = ExportStatus.Idle,
                    userMessage = "Export expired before saving. The last backup is staged — export again or use \"Import last export\".",
                )
            }
            return
        }
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
        try {
            val snapshot = backupSerializer.deserialize(payload)
            com.linknest.core.data.backup.validateBackup(snapshot)
            val current = exportDataUseCase()
            _uiState.update { it.copy(importStatus = ImportStatus.Preview(snapshot, current.websites.size)) }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            _uiState.update { it.copy(importStatus = ImportStatus.Failed(failure.message ?: "Invalid backup.")) }
        }
    }

    fun confirmImport() {
        val preview = uiState.value.importStatus as? ImportStatus.Preview ?: return
        _uiState.update { it.copy(importStatus = ImportStatus.Running) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val recovery = backupSerializer.serialize(exportDataUseCase())
                backupFileManager.stageBackup(recovery)
                val summary = importDataUseCase(preview.snapshot)
                _uiState.update { it.copy(
                    importStatus = ImportStatus.Success(summary.importedWebsites, summary.importedCategories),
                    stagedInfo = backupFileManager.getStagedBackupInfo(),
                    userMessage = (listOf("Restore complete. Previous library is staged for recovery.") + summary.warnings).joinToString("\n"),
                ) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _uiState.update { it.copy(importStatus = ImportStatus.Failed(failure.message ?: "Restore failed.")) }
            }
        }
    }

    fun restoreCategory(id: Long) {
        viewModelScope.launch {
            try { categoryRepository.archiveCategory(id, false) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) { _uiState.update { it.copy(userMessage = failure.message) } }
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
