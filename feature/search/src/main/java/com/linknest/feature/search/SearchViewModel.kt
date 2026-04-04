package com.linknest.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linknest.core.action.ActionResult
import com.linknest.core.action.action.TrackWebsiteOpenAction
import com.linknest.core.action.model.SearchPipelineInput
import com.linknest.core.action.model.SearchPipelineOutput
import com.linknest.core.action.pipeline.SearchPipeline
import com.linknest.core.data.usecase.ApplyFilterUseCase
import com.linknest.core.data.usecase.ClearRecentQueriesUseCase
import com.linknest.core.data.usecase.DeleteSavedFilterUseCase
import com.linknest.core.data.usecase.GetAllWebsiteItemsUseCase
import com.linknest.core.data.usecase.GetSearchSuggestionsUseCase
import com.linknest.core.data.usecase.LoadSavedFiltersUseCase
import com.linknest.core.data.usecase.ObserveRecentQueriesUseCase
import com.linknest.core.data.usecase.ObserveSelectableCategoriesUseCase
import com.linknest.core.data.usecase.ObserveTagsUseCase
import com.linknest.core.data.usecase.SaveFilterUseCase
import com.linknest.core.data.usecase.SaveRecentQueryUseCase
import com.linknest.core.data.usecase.WarmSearchIndexUseCase
import com.linknest.core.model.FollowUpStatus
import com.linknest.core.model.HealthStatus
import com.linknest.core.model.IconSource
import com.linknest.core.model.RecentQuery
import com.linknest.core.model.SavedFilter
import com.linknest.core.model.SavedFilterSpec
import com.linknest.core.model.SearchResultGroup
import com.linknest.core.model.SearchResultItem
import com.linknest.core.model.SearchResultModel
import com.linknest.core.model.SearchSuggestion
import com.linknest.core.model.SearchSuggestionType
import com.linknest.core.model.SelectableCategory
import com.linknest.core.model.TagModel
import com.linknest.core.model.WebsiteListItem
import com.linknest.core.model.WebsitePriority
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

private data class SearchRequest(
    val query: String,
    val spec: SavedFilterSpec,
)

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: SearchResultModel = SearchResultModel(),
    val savedFilters: List<SavedFilter> = emptyList(),
    val recentQueries: List<RecentQuery> = emptyList(),
    val suggestions: List<SearchSuggestion> = emptyList(),
    val availableCategories: List<SelectableCategory> = emptyList(),
    val availableTags: List<TagModel> = emptyList(),
    val selectedCategoryIds: Set<Long> = emptySet(),
    val selectedTagNames: Set<String> = emptySet(),
    val selectedHealthStatuses: Set<HealthStatus> = emptySet(),
    val selectedPriorities: Set<WebsitePriority> = emptySet(),
    val selectedFollowUpStatuses: Set<FollowUpStatus> = emptySet(),
    val pinnedOnly: Boolean = false,
    val recentOnly: Boolean = false,
    val duplicatesOnly: Boolean = false,
    val includeNeedsAttention: Boolean = false,
    val activeSavedFilterId: Long? = null,
    val activeSmartCollection: SearchSmartCollection? = null,
    val userMessage: String? = null,
) {
    val hasActiveFilters: Boolean
        get() = selectedCategoryIds.isNotEmpty() ||
            selectedTagNames.isNotEmpty() ||
            selectedHealthStatuses.isNotEmpty() ||
            selectedPriorities.isNotEmpty() ||
            selectedFollowUpStatuses.isNotEmpty() ||
            pinnedOnly ||
            recentOnly ||
            duplicatesOnly ||
            includeNeedsAttention
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val searchPipeline: SearchPipeline,
    private val trackWebsiteOpenAction: TrackWebsiteOpenAction,
    private val loadSavedFiltersUseCase: LoadSavedFiltersUseCase,
    private val saveFilterUseCase: SaveFilterUseCase,
    private val deleteSavedFilterUseCase: DeleteSavedFilterUseCase,
    private val observeRecentQueriesUseCase: ObserveRecentQueriesUseCase,
    private val clearRecentQueriesUseCase: ClearRecentQueriesUseCase,
    private val saveRecentQueryUseCase: SaveRecentQueryUseCase,
    private val getSearchSuggestionsUseCase: GetSearchSuggestionsUseCase,
    observeSelectableCategoriesUseCase: ObserveSelectableCategoriesUseCase,
    observeTagsUseCase: ObserveTagsUseCase,
    private val getAllWebsiteItemsUseCase: GetAllWebsiteItemsUseCase,
    private val applyFilterUseCase: ApplyFilterUseCase,
    private val warmSearchIndexUseCase: WarmSearchIndexUseCase,
) : ViewModel() {
    private val initialSavedFilterId = savedStateHandle
        .get<Long>(SEARCH_FILTER_ID_ARG)
        ?.takeIf { it > 0L }
    private val initialQuery = savedStateHandle.get<String>(SEARCH_QUERY_ARG).orEmpty()
    private val initialCollection = SearchSmartCollection.fromRouteValue(
        savedStateHandle.get<String>(SEARCH_COLLECTION_ARG),
    )

    private val requestFlow = MutableStateFlow(
        SearchRequest(
            query = initialQuery,
            spec = SavedFilterSpec(query = initialQuery),
        ),
    )
    private val suggestionQueryFlow = MutableStateFlow(initialQuery)

    private val _uiState = MutableStateFlow(SearchUiState(query = initialQuery))
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            warmSearchIndexUseCase()
        }

        requestFlow
            .debounce(220)
            .distinctUntilChanged()
            .onEach(::performSearch)
            .launchIn(viewModelScope)

        suggestionQueryFlow
            .debounce(120)
            .distinctUntilChanged()
            .onEach(::updateSuggestions)
            .launchIn(viewModelScope)

        viewModelScope.launch {
            observeSelectableCategoriesUseCase().collect { categories ->
                _uiState.update { it.copy(availableCategories = categories) }
                emitSearchRequest()
            }
        }

        viewModelScope.launch {
            observeTagsUseCase().collect { tags ->
                _uiState.update { it.copy(availableTags = tags) }
            }
        }

        viewModelScope.launch {
            observeRecentQueriesUseCase().collect { recentQueries ->
                _uiState.update { it.copy(recentQueries = recentQueries) }
                if (_uiState.value.query.isNotBlank()) {
                    updateSuggestions(_uiState.value.query)
                }
            }
        }

        viewModelScope.launch {
            loadSavedFiltersUseCase().collect { filters ->
                _uiState.update { current -> current.copy(savedFilters = filters) }
                if (_uiState.value.activeSavedFilterId == null) {
                    initialSavedFilterId?.let { filterId ->
                        filters.firstOrNull { it.id == filterId }?.let(::applySavedFilter)
                    }
                }
            }
        }

        initialCollection?.let { applySmartCollection(collection = it, emitRequest = true) }
    }

    fun onQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                query = query,
                activeSavedFilterId = null,
                activeSmartCollection = null,
            )
        }
        emitSearchRequest()
        suggestionQueryFlow.value = query
    }

    fun onSuggestionSelected(suggestion: SearchSuggestion) {
        val savedFilterId = suggestion.savedFilterId
        if (savedFilterId != null) {
            onSavedFilterSelected(savedFilterId)
            return
        }
        onQueryChanged(suggestion.query)
    }

    fun onRecentQuerySelected(recentQuery: RecentQuery) {
        onQueryChanged(recentQuery.query)
    }

    fun onClearRecentQueries() {
        viewModelScope.launch {
            runCatching { clearRecentQueriesUseCase() }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(userMessage = throwable.message ?: "Unable to clear recent queries.")
                    }
                }
        }
    }

    fun onCategoryToggled(categoryId: Long) {
        _uiState.update { current ->
            current.copy(
                selectedCategoryIds = current.selectedCategoryIds.toggle(categoryId),
                activeSavedFilterId = null,
                activeSmartCollection = null,
            )
        }
        emitSearchRequest()
    }

    fun onTagToggled(tagName: String) {
        _uiState.update { current ->
            current.copy(
                selectedTagNames = current.selectedTagNames.toggle(tagName),
                activeSavedFilterId = null,
                activeSmartCollection = null,
            )
        }
        emitSearchRequest()
    }

    fun onHealthStatusToggled(status: HealthStatus) {
        _uiState.update { current ->
            current.copy(
                selectedHealthStatuses = current.selectedHealthStatuses.toggle(status),
                activeSavedFilterId = null,
                activeSmartCollection = null,
            )
        }
        emitSearchRequest()
    }

    fun onPriorityToggled(priority: WebsitePriority) {
        _uiState.update { current ->
            current.copy(
                selectedPriorities = current.selectedPriorities.toggle(priority),
                activeSavedFilterId = null,
                activeSmartCollection = null,
            )
        }
        emitSearchRequest()
    }

    fun onFollowUpStatusToggled(status: FollowUpStatus) {
        _uiState.update { current ->
            current.copy(
                selectedFollowUpStatuses = current.selectedFollowUpStatuses.toggle(status),
                activeSavedFilterId = null,
                activeSmartCollection = null,
            )
        }
        emitSearchRequest()
    }

    fun onPinnedOnlyChanged(enabled: Boolean) {
        _uiState.update {
            it.copy(
                pinnedOnly = enabled,
                activeSavedFilterId = null,
                activeSmartCollection = null,
            )
        }
        emitSearchRequest()
    }

    fun onRecentOnlyChanged(enabled: Boolean) {
        _uiState.update {
            it.copy(
                recentOnly = enabled,
                activeSavedFilterId = null,
                activeSmartCollection = null,
            )
        }
        emitSearchRequest()
    }

    fun onDuplicatesOnlyChanged(enabled: Boolean) {
        _uiState.update {
            it.copy(
                duplicatesOnly = enabled,
                activeSavedFilterId = null,
                activeSmartCollection = null,
            )
        }
        emitSearchRequest()
    }

    fun onNeedsAttentionChanged(enabled: Boolean) {
        _uiState.update {
            it.copy(
                includeNeedsAttention = enabled,
                activeSavedFilterId = null,
                activeSmartCollection = null,
            )
        }
        emitSearchRequest()
    }

    fun onSavedFilterSelected(filterId: Long) {
        uiState.value.savedFilters.firstOrNull { it.id == filterId }?.let(::applySavedFilter)
    }

    fun onDeleteSavedFilter(filterId: Long) {
        viewModelScope.launch {
            runCatching { deleteSavedFilterUseCase(filterId) }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(userMessage = throwable.message ?: "Unable to delete saved search.")
                    }
                }
        }
    }

    fun onSaveCurrentFilter(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(userMessage = "Search name is required.") }
            return
        }
        viewModelScope.launch {
            runCatching {
                saveFilterUseCase(
                    name = trimmedName,
                    spec = currentFilterSpec(),
                )
            }.onSuccess { filter ->
                _uiState.update {
                    it.copy(
                        activeSavedFilterId = filter.id,
                        userMessage = "Saved search added.",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(userMessage = throwable.message ?: "Unable to save current search.")
                }
            }
        }
    }

    fun onApplySmartCollection(collection: SearchSmartCollection) {
        applySmartCollection(collection = collection, emitRequest = true)
    }

    fun onClearFilters() {
        _uiState.update {
            it.copy(
                activeSavedFilterId = null,
                activeSmartCollection = null,
                selectedCategoryIds = emptySet(),
                selectedTagNames = emptySet(),
                selectedHealthStatuses = emptySet(),
                selectedPriorities = emptySet(),
                selectedFollowUpStatuses = emptySet(),
                pinnedOnly = false,
                recentOnly = false,
                duplicatesOnly = false,
                includeNeedsAttention = false,
            )
        }
        emitSearchRequest()
    }

    fun onWebsiteOpened(websiteId: Long) {
        viewModelScope.launch {
            trackWebsiteOpenAction(websiteId)
        }
    }

    fun onMessageConsumed() {
        _uiState.update { it.copy(userMessage = null) }
    }

    private fun applySavedFilter(filter: SavedFilter) {
        _uiState.update {
            it.copy(
                query = filter.spec.query,
                selectedCategoryIds = filter.spec.categoryIds.toSet(),
                selectedTagNames = filter.spec.tagNames.toSet(),
                selectedHealthStatuses = filter.spec.healthStatuses.toSet(),
                selectedPriorities = filter.spec.priorities.toSet(),
                selectedFollowUpStatuses = filter.spec.followUpStatuses.toSet(),
                pinnedOnly = filter.spec.pinnedOnly,
                recentOnly = filter.spec.recentOnly,
                duplicatesOnly = filter.spec.duplicatesOnly,
                includeNeedsAttention = filter.spec.includeNeedsAttention,
                activeSavedFilterId = filter.id,
                activeSmartCollection = null,
            )
        }
        emitSearchRequest()
        suggestionQueryFlow.value = filter.spec.query
    }

    private fun applySmartCollection(
        collection: SearchSmartCollection,
        emitRequest: Boolean,
    ) {
        _uiState.update {
            it.copy(
                query = "",
                activeSavedFilterId = null,
                activeSmartCollection = collection,
                pinnedOnly = collection == SearchSmartCollection.PINNED,
                recentOnly = collection == SearchSmartCollection.RECENT,
                duplicatesOnly = collection == SearchSmartCollection.DUPLICATES,
                includeNeedsAttention = collection == SearchSmartCollection.NEEDS_ATTENTION,
                selectedCategoryIds = emptySet(),
                selectedTagNames = emptySet(),
                selectedHealthStatuses = emptySet(),
                selectedPriorities = emptySet(),
                selectedFollowUpStatuses = emptySet(),
            )
        }
        suggestionQueryFlow.value = ""
        if (emitRequest) {
            emitSearchRequest()
        }
    }

    private fun emitSearchRequest() {
        requestFlow.value = SearchRequest(
            query = uiState.value.query,
            spec = currentFilterSpec(),
        )
    }

    private fun currentFilterSpec(): SavedFilterSpec = uiState.value.let { state ->
        SavedFilterSpec(
            query = state.query,
            categoryIds = state.selectedCategoryIds.toList(),
            tagNames = state.selectedTagNames.toList(),
            healthStatuses = state.selectedHealthStatuses.toList(),
            pinnedOnly = state.pinnedOnly,
            recentOnly = state.recentOnly,
            followUpStatuses = state.selectedFollowUpStatuses.toList(),
            priorities = state.selectedPriorities.toList(),
            duplicatesOnly = state.duplicatesOnly,
            includeNeedsAttention = state.includeNeedsAttention,
        )
    }

    private suspend fun performSearch(request: SearchRequest) {
        val hasFilters = request.spec.hasActiveConstraints()
        if (request.query.isBlank() && !hasFilters) {
            _uiState.update {
                it.copy(
                    isSearching = false,
                    results = SearchResultModel(),
                )
            }
            return
        }

        _uiState.update { it.copy(isSearching = true) }

        val categoryNames = uiState.value.availableCategories.associate { it.id to it.name }
        if (request.query.isBlank()) {
            val filteredItems = applyFilterUseCase(
                getAllWebsiteItemsUseCase(),
                request.spec,
            )
            _uiState.update {
                it.copy(
                    isSearching = false,
                    results = buildResultsFromWebsiteItems(
                        items = filteredItems,
                        categoryNames = categoryNames,
                        query = request.spec.query,
                    ),
                )
            }
            return
        }

        val allowedIds = if (hasFilters) {
            applyFilterUseCase(
                getAllWebsiteItemsUseCase(),
                request.spec.copy(query = ""),
            ).map(WebsiteListItem::id).toSet()
        } else {
            null
        }

        when (val result = searchPipeline(SearchPipelineInput(query = request.query))) {
            is ActionResult.Success -> {
                saveRecentQueryUseCase(result.value.query)
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        results = filterSearchResultModel(result.value, allowedIds),
                    )
                }
            }
            is ActionResult.PartialSuccess -> {
                saveRecentQueryUseCase(result.value.query)
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        results = filterSearchResultModel(result.value, allowedIds),
                        userMessage = result.issues.joinToString("\n") { issue -> issue.message },
                    )
                }
            }
            is ActionResult.Failure -> {
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        userMessage = result.issue.message,
                    )
                }
            }
        }
    }

    private suspend fun updateSuggestions(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(suggestions = emptyList()) }
            return
        }

        val recentMatches = uiState.value.recentQueries
            .filter { recentQuery -> recentQuery.query.contains(query, ignoreCase = true) }
            .map { recentQuery ->
                SearchSuggestion(
                    id = "recent-${recentQuery.id}",
                    type = SearchSuggestionType.RECENT_QUERY,
                    title = recentQuery.query,
                    supportingText = "Recent query",
                    query = recentQuery.query,
                )
            }

        val indexSuggestions = getSearchSuggestionsUseCase(query)
        val merged = linkedMapOf<String, SearchSuggestion>()
        (recentMatches + indexSuggestions).forEach { suggestion ->
            merged.putIfAbsent("${suggestion.type}:${suggestion.title.lowercase()}", suggestion)
        }

        _uiState.update { it.copy(suggestions = merged.values.take(8)) }
    }

    private fun filterSearchResultModel(
        pipelineOutput: SearchPipelineOutput,
        allowedIds: Set<Long>?,
    ): SearchResultModel {
        val groups = pipelineOutput.groups.mapNotNull { group ->
            val filteredResults = if (allowedIds == null) {
                group.results
            } else {
                group.results.filter { result -> result.websiteId in allowedIds }
            }
            filteredResults.takeIf { it.isNotEmpty() }?.let {
                SearchResultGroup(title = group.title, results = it)
            }
        }
        return SearchResultModel(
            query = pipelineOutput.query,
            groups = groups,
            totalCount = groups.sumOf { it.results.size },
        )
    }

    private fun buildResultsFromWebsiteItems(
        items: List<WebsiteListItem>,
        categoryNames: Map<Long, String>,
        query: String,
    ): SearchResultModel {
        val groups = items
            .groupBy { item -> categoryNames[item.categoryId] ?: "Other" }
            .map { (categoryName, groupItems) ->
                SearchResultGroup(
                    title = categoryName,
                    results = groupItems.map { item -> item.asSearchResultItem(categoryName) },
                )
            }
            .sortedBy { it.title.lowercase() }

        return SearchResultModel(
            query = query,
            groups = groups,
            totalCount = groups.sumOf { it.results.size },
        )
    }

    private fun WebsiteListItem.asSearchResultItem(categoryName: String): SearchResultItem =
        SearchResultItem(
            websiteId = id,
            categoryId = categoryId,
            categoryName = categoryName,
            title = title,
            domain = domain,
            normalizedUrl = normalizedUrl,
            finalUrl = finalUrl,
            faviconUrl = faviconUrl,
            ogImageUrl = ogImageUrl,
            chosenIconSource = chosenIconSource,
            customIconUri = customIconUri,
            emojiIcon = emojiIcon,
            cachedIconUri = cachedIconUri,
            healthStatus = healthStatus,
            priority = priority,
            followUpStatus = followUpStatus,
            tagNames = tagNames,
            matchedFields = buildList {
                if (isPinned) add("pinned")
                if (duplicateCount > 0) add("duplicate")
                if (followUpStatus != FollowUpStatus.NONE) add("follow-up")
                if (priority != WebsitePriority.NORMAL) add("priority")
                if (!note.isNullOrBlank()) add("note")
            },
        )
}

private fun SavedFilterSpec.hasActiveConstraints(): Boolean = categoryIds.isNotEmpty() ||
    tagNames.isNotEmpty() ||
    healthStatuses.isNotEmpty() ||
    pinnedOnly ||
    recentOnly ||
    followUpStatuses.isNotEmpty() ||
    priorities.isNotEmpty() ||
    duplicatesOnly ||
    includeNeedsAttention

private fun <T> Set<T>.toggle(value: T): Set<T> =
    if (value in this) this - value else this + value
