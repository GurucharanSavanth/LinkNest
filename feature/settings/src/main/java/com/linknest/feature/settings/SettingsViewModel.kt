package com.linknest.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linknest.core.action.ActionResult
import com.linknest.core.action.model.BackupExportPipelineInput
import com.linknest.core.action.pipeline.BackupExportPipeline
import com.linknest.core.action.pipeline.HealthCheckPipeline
import com.linknest.core.action.pipeline.ImportRestorePipeline
import com.linknest.core.data.usecase.ObserveUserPreferencesUseCase
import com.linknest.core.data.usecase.UpdateBackgroundHealthChecksUseCase
import com.linknest.core.data.usecase.UpdateEncryptedBackupsUseCase
import com.linknest.core.data.usecase.UpdateTileDensityModeUseCase
import com.linknest.core.data.usecase.UpdateTileSizeUseCase
import com.linknest.core.model.HealthReportItem
import com.linknest.core.model.TileDensityMode
import com.linknest.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val preferences: UserPreferences = UserPreferences(),
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isRunningHealthCheck: Boolean = false,
    val backupJson: String = "",
    val backupFilePath: String? = null,
    val importPayload: String = "",
    val healthSummary: String? = null,
    val latestHealthReport: List<HealthReportItem> = emptyList(),
    val userMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeUserPreferencesUseCase: ObserveUserPreferencesUseCase,
    private val updateTileSizeUseCase: UpdateTileSizeUseCase,
    private val updateTileDensityModeUseCase: UpdateTileDensityModeUseCase,
    private val updateBackgroundHealthChecksUseCase: UpdateBackgroundHealthChecksUseCase,
    private val updateEncryptedBackupsUseCase: UpdateEncryptedBackupsUseCase,
    private val backupExportPipeline: BackupExportPipeline,
    private val importRestorePipeline: ImportRestorePipeline,
    private val healthCheckPipeline: HealthCheckPipeline,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeUserPreferencesUseCase().collect { preferences ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        preferences = preferences,
                    )
                }
            }
        }
    }

    fun onTileDensityModeSelected(tileDensityMode: TileDensityMode) {
        viewModelScope.launch {
            updateTileDensityModeUseCase(tileDensityMode)
        }
    }

    fun onTileSizeSelected(tileSizeDp: Int) {
        viewModelScope.launch {
            updateTileSizeUseCase(tileSizeDp)
        }
    }

    fun onImportPayloadChanged(payload: String) {
        _uiState.update { it.copy(importPayload = payload) }
    }

    fun onBackgroundHealthChecksChanged(enabled: Boolean) {
        viewModelScope.launch {
            updateBackgroundHealthChecksUseCase(enabled)
        }
    }

    fun onEncryptedBackupsChanged(enabled: Boolean) {
        viewModelScope.launch {
            updateEncryptedBackupsUseCase(enabled)
        }
    }

    fun onExportBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, userMessage = null) }
            when (
                val result = backupExportPipeline(
                    BackupExportPipelineInput(
                        encrypted = uiState.value.preferences.encryptedBackupsEnabled,
                    ),
                )
            ) {
                is ActionResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            backupJson = result.value.artifact.json,
                            backupFilePath = result.value.artifact.filePath,
                            userMessage = if (result.value.artifact.isEncrypted) {
                                "Encrypted backup exported."
                            } else {
                                "Backup exported."
                            },
                        )
                    }
                }
                is ActionResult.PartialSuccess -> {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            backupJson = result.value.artifact.json,
                            backupFilePath = result.value.artifact.filePath,
                            userMessage = result.issues.joinToString("\n") { issue -> issue.message },
                        )
                    }
                }
                is ActionResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            userMessage = result.issue.message,
                        )
                    }
                }
            }
        }
    }

    fun onImportBackup() {
        val payload = uiState.value.importPayload
        if (payload.isBlank()) {
            _uiState.update { it.copy(userMessage = "Paste a backup payload first.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, userMessage = null) }
            when (val result = importRestorePipeline(payload)) {
                is ActionResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            userMessage = "Import completed: ${result.value.summary.importedWebsites} websites restored.",
                        )
                    }
                }
                is ActionResult.PartialSuccess -> {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            userMessage = buildString {
                                append("Import completed with warnings.")
                                append('\n')
                                append(result.issues.joinToString("\n") { issue -> issue.message })
                            },
                        )
                    }
                }
                is ActionResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            userMessage = result.issue.message,
                        )
                    }
                }
            }
        }
    }

    fun onRunHealthCheck() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRunningHealthCheck = true, userMessage = null) }
            val staleBefore = System.currentTimeMillis() - (12L * 60L * 60L * 1000L)
            when (val result = healthCheckPipeline(staleBefore = staleBefore)) {
                is ActionResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isRunningHealthCheck = false,
                            healthSummary = "Checked ${result.value.checkedCount} links. " +
                                "OK ${result.value.okCount}, blocked ${result.value.blockedCount}, redirected ${result.value.redirectedCount}, " +
                                "dead ${result.value.deadCount}, timeout ${result.value.timeoutCount}.",
                            latestHealthReport = result.value.items,
                        )
                    }
                }
                is ActionResult.PartialSuccess -> {
                    _uiState.update {
                        it.copy(
                            isRunningHealthCheck = false,
                            healthSummary = "Checked ${result.value.checkedCount} links with warnings. " +
                                "OK ${result.value.okCount}, blocked ${result.value.blockedCount}, redirected ${result.value.redirectedCount}, " +
                                "dead ${result.value.deadCount}, timeout ${result.value.timeoutCount}.",
                            latestHealthReport = result.value.items,
                            userMessage = result.issues.joinToString("\n") { issue -> issue.message },
                        )
                    }
                }
                is ActionResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isRunningHealthCheck = false,
                            userMessage = result.issue.message,
                        )
                    }
                }
            }
        }
    }

    fun onMessageConsumed() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
