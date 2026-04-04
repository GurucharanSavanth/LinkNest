package com.linknest.feature.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linknest.core.action.ActionResult
import com.linknest.core.action.pipeline.HealthCheckPipeline
import com.linknest.core.data.usecase.GetAllWebsiteItemsUseCase
import com.linknest.core.model.HealthReportItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HealthReportUiState(
    val isLoading: Boolean = true,
    val isRunning: Boolean = false,
    val items: List<HealthReportItem> = emptyList(),
    val summary: String? = null,
    val userMessage: String? = null,
)

@HiltViewModel
class HealthReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAllWebsiteItemsUseCase: GetAllWebsiteItemsUseCase,
    private val healthCheckPipeline: HealthCheckPipeline,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HealthReportUiState())
    val uiState: StateFlow<HealthReportUiState> = _uiState.asStateFlow()

    init {
        if (savedStateHandle.get<Boolean>(HEALTH_REPORT_AUTO_RUN_ARG) == true) {
            runHealthCheck()
        } else {
            refreshStoredReport()
        }
    }

    fun refreshStoredReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val items = getAllWebsiteItemsUseCase()
                .filter { item -> item.lastCheckedAt != null || item.healthStatus != com.linknest.core.model.HealthStatus.UNKNOWN }
                .sortedByDescending { item -> item.lastCheckedAt ?: 0L }
                .map { item ->
                    HealthReportItem(
                        websiteId = item.id,
                        title = item.title,
                        normalizedUrl = item.normalizedUrl,
                        status = item.healthStatus,
                    )
                }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    items = items,
                )
            }
        }
    }

    fun runHealthCheck() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRunning = true, userMessage = null) }
            val staleBefore = System.currentTimeMillis() - (12L * 60L * 60L * 1000L)
            when (val result = healthCheckPipeline(staleBefore = staleBefore)) {
                is ActionResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isRunning = false,
                            items = result.value.items,
                            summary = buildSummary(result.value),
                        )
                    }
                }
                is ActionResult.PartialSuccess -> {
                    _uiState.update {
                        it.copy(
                            isRunning = false,
                            items = result.value.items,
                            summary = buildSummary(result.value),
                            userMessage = result.issues.joinToString("\n") { issue -> issue.message },
                        )
                    }
                }
                is ActionResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isRunning = false,
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

    private fun buildSummary(output: com.linknest.core.action.model.HealthCheckPipelineOutput): String =
        "Checked ${output.checkedCount} links. Good ${output.okCount}, caution " +
            "${output.blockedCount + output.redirectedCount + output.loginRequiredCount + output.dnsFailedCount + output.sslIssueCount}, " +
            "failed ${output.deadCount + output.timeoutCount}."
}
