package com.linknest.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linknest.core.action.ActionResult
import com.linknest.core.action.action.GenerateIntegrityOverviewAction
import com.linknest.core.action.model.IntegrityOverviewInput
import com.linknest.core.model.IntegrityOverview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IntegrityCenterUiState(
    val isLoading: Boolean = true,
    val overview: IntegrityOverview = IntegrityOverview(),
    val userMessage: String? = null,
)

@HiltViewModel
class IntegrityCenterViewModel @Inject constructor(
    private val generateIntegrityOverviewAction: GenerateIntegrityOverviewAction,
) : ViewModel() {
    private val _uiState = MutableStateFlow(IntegrityCenterUiState())
    val uiState: StateFlow<IntegrityCenterUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, userMessage = null) }
            when (val result = generateIntegrityOverviewAction(IntegrityOverviewInput(refresh = true))) {
                is ActionResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            overview = result.value.overview,
                        )
                    }
                }
                is ActionResult.PartialSuccess -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            overview = result.value.overview,
                            userMessage = result.issues.joinToString("\n") { issue -> issue.message },
                        )
                    }
                }
                is ActionResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
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
