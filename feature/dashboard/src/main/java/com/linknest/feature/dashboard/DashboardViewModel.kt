package com.linknest.feature.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linknest.core.action.ActionResult
import com.linknest.core.action.action.ArchiveCategoryAction
import com.linknest.core.action.action.ArchiveCategoryInput
import com.linknest.core.action.action.DeleteCategoryAction
import com.linknest.core.action.action.DeleteWebsiteAction
import com.linknest.core.action.action.MoveWebsiteToCategoryAction
import com.linknest.core.action.action.MoveWebsiteToCategoryInput
import com.linknest.core.action.action.ReorderCategoryAction
import com.linknest.core.action.action.ReorderWebsiteAction
import com.linknest.core.action.action.SetWebsitePinnedAction
import com.linknest.core.action.action.SetWebsitePinnedInput
import com.linknest.core.action.action.TrackWebsiteOpenAction
import com.linknest.core.action.action.UpdateCategoryAction
import com.linknest.core.action.action.UpdateCategoryInput
import com.linknest.core.action.model.ReorderWebsiteInput
import com.linknest.core.data.usecase.ObserveDashboardUseCase
import com.linknest.core.data.usecase.SeedDefaultCategoryUseCase
import com.linknest.core.data.usecase.ToggleCategoryCollapsedUseCase
import com.linknest.core.data.usecase.UpdateLayoutModeUseCase
import com.linknest.core.model.DashboardModel
import com.linknest.core.model.LayoutMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val dashboard: DashboardModel = DashboardModel(),
    val searchQuery: String = "",
    val focusedCategoryId: Long? = null,
    val focusedWebsiteId: Long? = null,
    val userMessage: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeDashboardUseCase: ObserveDashboardUseCase,
    private val seedDefaultCategoryUseCase: SeedDefaultCategoryUseCase,
    private val toggleCategoryCollapsedUseCase: ToggleCategoryCollapsedUseCase,
    private val updateLayoutModeUseCase: UpdateLayoutModeUseCase,
    private val trackWebsiteOpenAction: TrackWebsiteOpenAction,
    private val reorderCategoryAction: ReorderCategoryAction,
    private val reorderWebsiteAction: ReorderWebsiteAction,
    private val moveWebsiteToCategoryAction: MoveWebsiteToCategoryAction,
    private val deleteWebsiteAction: DeleteWebsiteAction,
    private val setWebsitePinnedAction: SetWebsitePinnedAction,
    private val updateCategoryAction: UpdateCategoryAction,
    private val archiveCategoryAction: ArchiveCategoryAction,
    private val deleteCategoryAction: DeleteCategoryAction,
) : ViewModel() {
    private val deepLinkedCategoryId = savedStateHandle.get<Long>(DASHBOARD_CATEGORY_ID_ARG)?.takeIf { it > 0L }
    private val deepLinkedWebsiteId = savedStateHandle.get<Long>(DASHBOARD_WEBSITE_ID_ARG)?.takeIf { it > 0L }
    private val queryFlow = MutableStateFlow("")
    private var sourceDashboard: DashboardModel = DashboardModel()

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            focusedCategoryId = deepLinkedCategoryId,
            focusedWebsiteId = deepLinkedWebsiteId,
        ),
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            seedDefaultCategoryUseCase()
        }
        viewModelScope.launch {
            observeDashboardUseCase().collect { dashboard ->
                sourceDashboard = dashboard
                refreshDisplayedDashboard()
            }
        }
        queryFlow
            .debounce(180)
            .distinctUntilChanged()
            .onEach { refreshDisplayedDashboard() }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        queryFlow.value = query
    }

    fun onLayoutModeChanged(layoutMode: LayoutMode) {
        viewModelScope.launch {
            updateLayoutModeUseCase(layoutMode)
        }
    }

    fun onToggleCategoryCollapsed(categoryId: Long) {
        viewModelScope.launch {
            runCatching {
                toggleCategoryCollapsedUseCase(categoryId)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(userMessage = throwable.message ?: "Unable to update category state.")
                }
            }
        }
    }

    fun onWebsiteOpened(websiteId: Long) {
        viewModelScope.launch {
            trackWebsiteOpenAction(websiteId)
        }
    }

    fun onMoveCategory(categoryId: Long, direction: Int) {
        val current = uiState.value.dashboard.categories.map { it.id }.toMutableList()
        val currentIndex = current.indexOf(categoryId)
        if (currentIndex == -1) return
        val targetIndex = (currentIndex + direction).coerceIn(0, current.lastIndex)
        onMoveCategoryToIndex(categoryId, targetIndex)
    }

    fun onMoveCategoryToIndex(categoryId: Long, targetIndex: Int) {
        val current = uiState.value.dashboard.categories.map { it.id }.toMutableList()
        val currentIndex = current.indexOf(categoryId)
        if (currentIndex == -1) return
        val boundedTargetIndex = targetIndex.coerceIn(0, current.lastIndex)
        if (boundedTargetIndex == currentIndex) return
        current.removeAt(currentIndex)
        current.add(boundedTargetIndex, categoryId)

        viewModelScope.launch {
            when (val result = reorderCategoryAction(current)) {
                is ActionResult.Failure -> _uiState.update { it.copy(userMessage = result.issue.message) }
                else -> Unit
            }
        }
    }

    fun onMoveWebsite(categoryId: Long, websiteId: Long, direction: Int) {
        val category = uiState.value.dashboard.categories.firstOrNull { it.id == categoryId } ?: return
        val current = category.websites.map { it.id }.toMutableList()
        val currentIndex = current.indexOf(websiteId)
        if (currentIndex == -1) return
        val targetIndex = (currentIndex + direction).coerceIn(0, current.lastIndex)
        onMoveWebsiteToIndex(categoryId, websiteId, targetIndex)
    }

    fun onMoveWebsiteToIndex(categoryId: Long, websiteId: Long, targetIndex: Int) {
        val category = uiState.value.dashboard.categories.firstOrNull { it.id == categoryId } ?: return
        val current = category.websites.map { it.id }.toMutableList()
        val currentIndex = current.indexOf(websiteId)
        if (currentIndex == -1) return
        val boundedTargetIndex = targetIndex.coerceIn(0, current.lastIndex)
        if (boundedTargetIndex == currentIndex) return
        current.removeAt(currentIndex)
        current.add(boundedTargetIndex, websiteId)

        viewModelScope.launch {
            when (
                val result = reorderWebsiteAction(
                    ReorderWebsiteInput(
                        categoryId = categoryId,
                        orderedIds = current,
                    ),
                )
            ) {
                is ActionResult.Failure -> _uiState.update { it.copy(userMessage = result.issue.message) }
                else -> Unit
            }
        }
    }

    fun onMoveWebsiteToCategory(websiteId: Long, targetCategoryId: Long) {
        viewModelScope.launch {
            when (
                val result = moveWebsiteToCategoryAction(
                    MoveWebsiteToCategoryInput(
                        websiteId = websiteId,
                        targetCategoryId = targetCategoryId,
                    ),
                )
            ) {
                is ActionResult.Failure -> _uiState.update { it.copy(userMessage = result.issue.message) }
                else -> Unit
            }
        }
    }

    fun onDeleteWebsite(websiteId: Long) {
        viewModelScope.launch {
            when (val result = deleteWebsiteAction(websiteId)) {
                is ActionResult.Failure -> _uiState.update { it.copy(userMessage = result.issue.message) }
                else -> Unit
            }
        }
    }

    fun onSetPinned(websiteId: Long, pinned: Boolean) {
        viewModelScope.launch {
            when (
                val result = setWebsitePinnedAction(
                    SetWebsitePinnedInput(
                        websiteId = websiteId,
                        pinned = pinned,
                    ),
                )
            ) {
                is ActionResult.Failure -> _uiState.update { it.copy(userMessage = result.issue.message) }
                else -> Unit
            }
        }
    }

    fun onUpdateCategory(
        categoryId: Long,
        name: String,
        colorHex: String,
        iconValue: String?,
    ) {
        viewModelScope.launch {
            when (
                val result = updateCategoryAction(
                    UpdateCategoryInput(
                        categoryId = categoryId,
                        name = name,
                        colorHex = colorHex,
                        iconValue = iconValue,
                    ),
                )
            ) {
                is ActionResult.Failure -> _uiState.update { it.copy(userMessage = result.issue.message) }
                else -> Unit
            }
        }
    }

    fun onArchiveCategory(categoryId: Long) {
        viewModelScope.launch {
            when (
                val result = archiveCategoryAction(
                    ArchiveCategoryInput(
                        categoryId = categoryId,
                        archived = true,
                    ),
                )
            ) {
                is ActionResult.Failure -> _uiState.update { it.copy(userMessage = result.issue.message) }
                else -> Unit
            }
        }
    }

    fun onDeleteCategory(categoryId: Long) {
        viewModelScope.launch {
            when (val result = deleteCategoryAction(categoryId)) {
                is ActionResult.Failure -> _uiState.update { it.copy(userMessage = result.issue.message) }
                else -> Unit
            }
        }
    }

    fun onMessageConsumed() {
        _uiState.update { it.copy(userMessage = null) }
    }

    private fun refreshDisplayedDashboard() {
        val query = _uiState.value.searchQuery.trim().lowercase()
        val dashboard = if (query.isBlank()) {
            sourceDashboard
        } else {
            filterDashboard(sourceDashboard, query)
        }
        _uiState.update {
            it.copy(
                isLoading = false,
                dashboard = dashboard,
            )
        }
    }

    private fun filterDashboard(
        dashboard: DashboardModel,
        query: String,
    ): DashboardModel {
        val filteredCategories = dashboard.categories.mapNotNull { category ->
            val categoryMatches = category.name.lowercase().contains(query)
            val filteredWebsites = category.websites.filter { website ->
                categoryMatches ||
                    website.matchesDashboardQuery(query)
            }
            if (categoryMatches || filteredWebsites.isNotEmpty()) {
                category.copy(websites = filteredWebsites)
            } else {
                null
            }
        }
        val filteredSections = dashboard.smartSections.map { section ->
            section.copy(
                websites = section.websites.filter { website ->
                    website.matchesDashboardQuery(query)
                },
            )
        }.filter { it.websites.isNotEmpty() }

        return dashboard.copy(
            categories = filteredCategories,
            smartSections = filteredSections,
        )
    }
}
