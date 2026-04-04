package com.linknest.app.launch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linknest.core.action.ActionResult
import com.linknest.core.action.model.HealthCheckProgress
import com.linknest.core.action.model.HealthCheckPipelineOutput
import com.linknest.core.action.pipeline.HealthCheckPipeline
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LaunchGateUiState(
    val isChecking: Boolean = false,
    val progress: HealthCheckProgress? = null,
    val report: HealthCheckPipelineOutput? = null,
    val userMessage: String? = null,
)

@HiltViewModel
class LaunchGateViewModel @Inject constructor(
    private val healthCheckPipeline: HealthCheckPipeline,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LaunchGateUiState())
    val uiState: StateFlow<LaunchGateUiState> = _uiState.asStateFlow()

    private val _navigateToDashboard = Channel<Unit>(Channel.BUFFERED)
    val navigateToDashboard = _navigateToDashboard.receiveAsFlow()

    private var healthCheckJob: Job? = null

    fun onRunHealthCheck() {
        if (healthCheckJob?.isActive == true) return
        healthCheckJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isChecking = true,
                    progress = null,
                    report = null,
                    userMessage = null,
                )
            }
            when (
                val result = healthCheckPipeline(
                    staleBefore = Long.MAX_VALUE,
                    limit = Int.MAX_VALUE,
                    onProgress = { progress ->
                        _uiState.update { state -> state.copy(progress = progress) }
                    },
                )
            ) {
                is ActionResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            report = result.value,
                        )
                    }
                }
                is ActionResult.PartialSuccess -> {
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            report = result.value,
                            userMessage = result.issues.joinToString("\n") { issue -> issue.message },
                        )
                    }
                }
                is ActionResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            userMessage = result.issue.message,
                        )
                    }
                }
            }
        }
    }

    fun onSkipToDashboard() {
        healthCheckJob?.cancel()
        _uiState.update { it.copy(isChecking = false) }
        _navigateToDashboard.trySend(Unit)
    }

    fun onMessageConsumed() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
