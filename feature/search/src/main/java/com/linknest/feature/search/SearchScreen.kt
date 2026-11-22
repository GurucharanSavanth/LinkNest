package com.linknest.feature.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linknest.core.designsystem.component.GlassPanel
import com.linknest.core.designsystem.component.LinkNestGradientBackground
import com.linknest.core.designsystem.labelResource
import com.linknest.core.model.FollowUpStatus
import com.linknest.core.model.HealthStatus
import com.linknest.core.model.RecentQuery
import com.linknest.core.model.SearchResultGroup
import com.linknest.core.model.SearchResultItem
import com.linknest.core.model.SavedFilter
import com.linknest.core.model.SearchSuggestion
import com.linknest.core.model.SearchSuggestionType
import com.linknest.core.model.SelectableCategory
import com.linknest.core.model.TagModel
import com.linknest.core.model.WebsitePriority

@Composable
fun SearchRoute(
    onBack: () -> Unit,
    onOpenWebsite: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onMessageConsumed()
        }
    }

    SearchScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        onBack = onBack,
        onQueryChanged = viewModel::onQueryChanged,
        onSuggestionSelected = viewModel::onSuggestionSelected,
        onRecentQuerySelected = viewModel::onRecentQuerySelected,
        onClearRecentQueries = viewModel::onClearRecentQueries,
        onToggleCategory = viewModel::onCategoryToggled,
        onToggleTag = viewModel::onTagToggled,
        onToggleHealthStatus = viewModel::onHealthStatusToggled,
        onTogglePriority = viewModel::onPriorityToggled,
        onToggleFollowUpStatus = viewModel::onFollowUpStatusToggled,
        onPinnedOnlyChanged = viewModel::onPinnedOnlyChanged,
        onRecentOnlyChanged = viewModel::onRecentOnlyChanged,
        onDuplicatesOnlyChanged = viewModel::onDuplicatesOnlyChanged,
        onNeedsAttentionChanged = viewModel::onNeedsAttentionChanged,
        onApplySmartCollection = viewModel::onApplySmartCollection,
        onSaveCurrentFilter = viewModel::onSaveCurrentFilter,
        onSelectSavedFilter = viewModel::onSavedFilterSelected,
        onDeleteSavedFilter = viewModel::onDeleteSavedFilter,
        onClearFilters = viewModel::onClearFilters,
        onOpenWebsite = { website ->
            viewModel.onWebsiteOpened(website.websiteId)
            onOpenWebsite(website.websiteId, website.normalizedUrl)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SearchScreen(
    uiState: SearchUiState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onSuggestionSelected: (SearchSuggestion) -> Unit,
    onRecentQuerySelected: (RecentQuery) -> Unit,
    onClearRecentQueries: () -> Unit,
    onToggleCategory: (Long) -> Unit,
    onToggleTag: (String) -> Unit,
    onToggleHealthStatus: (HealthStatus) -> Unit,
    onTogglePriority: (WebsitePriority) -> Unit,
    onToggleFollowUpStatus: (FollowUpStatus) -> Unit,
    onPinnedOnlyChanged: (Boolean) -> Unit,
    onRecentOnlyChanged: (Boolean) -> Unit,
    onDuplicatesOnlyChanged: (Boolean) -> Unit,
    onNeedsAttentionChanged: (Boolean) -> Unit,
    onApplySmartCollection: (SearchSmartCollection) -> Unit,
    onSaveCurrentFilter: (String) -> Unit,
    onSelectSavedFilter: (Long) -> Unit,
    onDeleteSavedFilter: (Long) -> Unit,
    onClearFilters: () -> Unit,
    onOpenWebsite: (SearchResultItem) -> Unit,
) {
    val queryFocusRequester = remember { FocusRequester() }
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        queryFocusRequester.requestFocus()
    }

    if (showSaveDialog) {
        SaveFilterDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                onSaveCurrentFilter(name)
                showSaveDialog = false
            },
        )
    }

    LinkNestGradientBackground(
        modifier = modifier
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                when {
                    event.isCtrlPressed && (event.key == Key.K || event.key == Key.L) -> {
                        queryFocusRequester.requestFocus()
                        true
                    }
                    event.isCtrlPressed && event.key == Key.S -> {
                        showSaveDialog = true
                        true
                    }
                    event.key == Key.Escape -> {
                        onBack()
                        true
                    }
                    else -> false
                }
            },
    ) {
        BoxWithConstraints {
            val filtersAlwaysVisible = maxWidth >= 920.dp
            Scaffold(
                modifier = Modifier.imePadding(),
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.search_title)) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.search_back))
                            }
                        },
                        actions = {
                            val filterState = stringResource(if (filtersExpanded || filtersAlwaysVisible) R.string.search_expanded else R.string.search_collapsed)
                            IconButton(
                                enabled = !filtersAlwaysVisible,
                                modifier = Modifier.semantics {
                                    stateDescription = filterState
                                },
                                onClick = { filtersExpanded = !filtersExpanded },
                            ) {
                                Icon(Icons.Rounded.FilterAlt, contentDescription = stringResource(R.string.search_toggle_filters))
                            }
                        },
                    )
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    val showFilterBuilder = filtersExpanded || filtersAlwaysVisible
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                GlassPanel {
                                    OutlinedTextField(
                                        value = uiState.query,
                                        onValueChange = onQueryChanged,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(queryFocusRequester),
                                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                        label = { Text(stringResource(R.string.search_offline)) },
                                        placeholder = { Text(stringResource(R.string.search_hint)) },
                                        singleLine = true,
                                    )
                                }

                                SearchSuggestionsPanel(
                                    query = uiState.query,
                                    recentQueries = uiState.recentQueries,
                                    suggestions = uiState.suggestions,
                                    onSuggestionSelected = onSuggestionSelected,
                                    onRecentQuerySelected = onRecentQuerySelected,
                                    onClearRecentQueries = onClearRecentQueries,
                                )

                                SmartCollectionRow(
                                    activeCollection = uiState.activeSmartCollection,
                                    onApplySmartCollection = onApplySmartCollection,
                                )

                                SavedFiltersPanel(
                                    savedFilters = uiState.savedFilters,
                                    activeSavedFilterId = uiState.activeSavedFilterId,
                                    onSelectSavedFilter = onSelectSavedFilter,
                                    onDeleteSavedFilter = onDeleteSavedFilter,
                                    onSaveCurrentFilter = { showSaveDialog = true },
                                )

                                AnimatedVisibility(visible = showFilterBuilder) {
                                    SearchFilterPanel(
                                        uiState = uiState,
                                        onToggleCategory = onToggleCategory,
                                        onToggleTag = onToggleTag,
                                        onToggleHealthStatus = onToggleHealthStatus,
                                        onTogglePriority = onTogglePriority,
                                        onToggleFollowUpStatus = onToggleFollowUpStatus,
                                        onPinnedOnlyChanged = onPinnedOnlyChanged,
                                        onRecentOnlyChanged = onRecentOnlyChanged,
                                        onDuplicatesOnlyChanged = onDuplicatesOnlyChanged,
                                        onNeedsAttentionChanged = onNeedsAttentionChanged,
                                        onClearFilters = onClearFilters,
                                    )
                                }
                            }
                        }

                        if (uiState.isSearching) {
                            item(key = "search-loading") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }

                        if (!uiState.isSearching && uiState.results.totalCount == 0) {
                            item(key = "search-empty") {
                                GlassPanel {
                                    Text(
                                        text = if (uiState.query.isBlank() && !uiState.hasActiveFilters) {
                                            stringResource(R.string.search_empty_intro)
                                        } else {
                                            stringResource(R.string.search_no_results)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        uiState.results.groups.forEach { group ->
                            item(key = "group-${group.title}") { Text(group.title, style = MaterialTheme.typography.titleMedium) }
                            items(group.results, key = { "result-${it.websiteId}" }) { result ->
                                SearchGroup(group.copy(results = listOf(result)), onOpenWebsite)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartCollectionRow(
    activeCollection: SearchSmartCollection?,
    onApplySmartCollection: (SearchSmartCollection) -> Unit,
) {
    GlassPanel {
        Text(
            text = stringResource(R.string.search_smart_collections),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SearchSmartCollection.entries.forEach { collection ->
                val label = stringResource(collection.labelResource)
                val collectionDescription = stringResource(R.string.search_smart_collection_description, label)
                FilterChip(
                    modifier = Modifier.semantics { contentDescription = collectionDescription },
                    selected = activeCollection == collection,
                    onClick = { onApplySmartCollection(collection) },
                    label = { Text(label, modifier = Modifier.clearAndSetSemantics {}) },
                    leadingIcon = {
                        Icon(
                            imageVector = when (collection) {
                                SearchSmartCollection.PINNED -> Icons.Rounded.PushPin
                                SearchSmartCollection.RECENT -> Icons.Rounded.BookmarkAdded
                                SearchSmartCollection.DUPLICATES -> Icons.Rounded.Search
                                SearchSmartCollection.NEEDS_ATTENTION -> Icons.Rounded.WarningAmber
                            },
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SavedFiltersPanel(
    savedFilters: List<SavedFilter>,
    activeSavedFilterId: Long?,
    onSelectSavedFilter: (Long) -> Unit,
    onDeleteSavedFilter: (Long) -> Unit,
    onSaveCurrentFilter: () -> Unit,
) {
    GlassPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.search_saved_searches),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedButton(onClick = onSaveCurrentFilter) {
                Text(stringResource(R.string.search_save_search))
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (savedFilters.isEmpty()) {
            Text(
                text = stringResource(R.string.search_no_saved_searches),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                savedFilters.forEach { filter ->
                    FilterChip(
                        selected = activeSavedFilterId == filter.id,
                        onClick = { onSelectSavedFilter(filter.id) },
                        label = { Text(filter.name) },
                        trailingIcon = {
                            IconButton(onClick = { onDeleteSavedFilter(filter.id) }) { Icon(
                                imageVector = Icons.Rounded.ClearAll,
                                contentDescription = stringResource(R.string.search_delete_filter),
                            ) }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchSuggestionsPanel(
    query: String,
    recentQueries: List<RecentQuery>,
    suggestions: List<SearchSuggestion>,
    onSuggestionSelected: (SearchSuggestion) -> Unit,
    onRecentQuerySelected: (RecentQuery) -> Unit,
    onClearRecentQueries: () -> Unit,
) {
    val showRecentQueries = query.isBlank() && recentQueries.isNotEmpty()
    val showSuggestions = suggestions.isNotEmpty()
    if (!showRecentQueries && !showSuggestions) return

    GlassPanel {
        if (showRecentQueries) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.search_recent_queries),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = onClearRecentQueries) {
                    Text(stringResource(R.string.search_clear_history))
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                recentQueries.forEach { recentQuery ->
                    AssistChip(
                        onClick = { onRecentQuerySelected(recentQuery) },
                        label = { Text(recentQuery.query) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
        }

        if (showRecentQueries && showSuggestions) {
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (showSuggestions) {
            Text(
                text = stringResource(R.string.search_suggestions),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                suggestions.forEach { suggestion ->
                    AssistChip(
                        onClick = { onSuggestionSelected(suggestion) },
                        label = { Text(suggestion.title) },
                        leadingIcon = {
                            Icon(
                                imageVector = when (suggestion.type) {
                                    SearchSuggestionType.SAVED_SEARCH -> Icons.Rounded.BookmarkAdded
                                    SearchSuggestionType.CATEGORY -> Icons.AutoMirrored.Rounded.Label
                                    SearchSuggestionType.TAG -> Icons.AutoMirrored.Rounded.Label
                                    SearchSuggestionType.DOMAIN -> Icons.Rounded.Search
                                    SearchSuggestionType.NOTE -> Icons.Rounded.WarningAmber
                                    SearchSuggestionType.RECENT_QUERY -> Icons.Rounded.Search
                                    SearchSuggestionType.WEBSITE_TITLE -> Icons.Rounded.Search
                                    SearchSuggestionType.HEALTH -> Icons.Rounded.WarningAmber
                                    SearchSuggestionType.FLAG -> Icons.Rounded.FilterAlt
                                },
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchFilterPanel(
    uiState: SearchUiState,
    onToggleCategory: (Long) -> Unit,
    onToggleTag: (String) -> Unit,
    onToggleHealthStatus: (HealthStatus) -> Unit,
    onTogglePriority: (WebsitePriority) -> Unit,
    onToggleFollowUpStatus: (FollowUpStatus) -> Unit,
    onPinnedOnlyChanged: (Boolean) -> Unit,
    onRecentOnlyChanged: (Boolean) -> Unit,
    onDuplicatesOnlyChanged: (Boolean) -> Unit,
    onNeedsAttentionChanged: (Boolean) -> Unit,
    onClearFilters: () -> Unit,
) {
    GlassPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.search_filter_builder),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = onClearFilters, enabled = uiState.hasActiveFilters) {
                Text(stringResource(R.string.search_clear))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        FilterSection(
            title = stringResource(R.string.search_quick_flags),
            content = {
                ToggleChip(stringResource(R.string.search_pinned), uiState.pinnedOnly) { onPinnedOnlyChanged(!uiState.pinnedOnly) }
                ToggleChip(stringResource(R.string.search_recent), uiState.recentOnly) { onRecentOnlyChanged(!uiState.recentOnly) }
                ToggleChip(stringResource(R.string.search_duplicates), uiState.duplicatesOnly) { onDuplicatesOnlyChanged(!uiState.duplicatesOnly) }
                ToggleChip(stringResource(R.string.search_needs_attention), uiState.includeNeedsAttention) { onNeedsAttentionChanged(!uiState.includeNeedsAttention) }
            },
        )
        FilterSection(
            title = stringResource(R.string.search_categories),
            content = {
                uiState.availableCategories.forEach { category ->
                    ToggleChip(
                        label = category.name,
                        selected = category.id in uiState.selectedCategoryIds,
                    ) { onToggleCategory(category.id) }
                }
            },
        )
        FilterSection(
            title = stringResource(R.string.search_tags),
            content = {
                uiState.availableTags.take(18).forEach { tag ->
                    ToggleChip(
                        label = tag.name,
                        selected = tag.name in uiState.selectedTagNames,
                    ) { onToggleTag(tag.name) }
                }
            },
        )
        FilterSection(
            title = stringResource(R.string.search_health),
            content = {
                HealthStatus.entries.forEach { status ->
                    if (status != HealthStatus.UNKNOWN) {
                        ToggleChip(
                            label = stringResource(status.labelResource),
                            selected = status in uiState.selectedHealthStatuses,
                        ) { onToggleHealthStatus(status) }
                    }
                }
            },
        )
        FilterSection(
            title = stringResource(R.string.search_priority),
            content = {
                WebsitePriority.entries.forEach { priority ->
                    ToggleChip(
                        label = stringResource(priority.labelResource),
                        selected = priority in uiState.selectedPriorities,
                    ) { onTogglePriority(priority) }
                }
            },
        )
        FilterSection(
            title = stringResource(R.string.search_follow_up),
            content = {
                FollowUpStatus.entries.forEach { status ->
                    if (status != FollowUpStatus.NONE) {
                        ToggleChip(
                            label = stringResource(status.labelResource),
                            selected = status in uiState.selectedFollowUpStatuses,
                        ) { onToggleFollowUpStatus(status) }
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun ToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun SearchGroup(
    group: SearchResultGroup,
    onOpenWebsite: (SearchResultItem) -> Unit,
) {
    GlassPanel {
        Text(
            text = group.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            group.results.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenWebsite(item) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.title, fontWeight = FontWeight.Medium)
                        Text(
                            text = item.normalizedUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (item.tagNames.isNotEmpty()) {
                            Text(
                                text = item.tagNames.joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (item.matchedFields.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.search_matched_fields, item.matchedFields.map { field -> matchedFieldLabel(field) }.joinToString()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                    Text(
                        text = stringResource(item.healthStatus.labelResource),
                        style = MaterialTheme.typography.labelMedium,
                        color = item.healthStatus.color,
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveFilterDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.search_save_filter_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_filter_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }) {
                Text(stringResource(R.string.search_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.search_cancel))
            }
        },
    )
}

private val HealthStatus.color: Color
    get() = when (this) {
        HealthStatus.OK -> Color(0xFF66BB6A)
        HealthStatus.LOGIN_REQUIRED,
        HealthStatus.BLOCKED,
        HealthStatus.REDIRECTED,
        HealthStatus.DNS_FAILED,
        HealthStatus.SSL_ISSUE,
        -> Color(0xFFFFC107)
        HealthStatus.DEAD,
        HealthStatus.TIMEOUT,
        -> Color(0xFFEF5350)
        HealthStatus.UNKNOWN -> Color(0xFF90A4AE)
    }

@Composable
private fun matchedFieldLabel(field: String): String = when (field) {
    "title" -> stringResource(R.string.search_match_title)
    "domain" -> stringResource(R.string.search_match_domain)
    "url" -> stringResource(R.string.search_match_url)
    "final" -> stringResource(R.string.search_match_final)
    "category" -> stringResource(R.string.search_match_category)
    "tag" -> stringResource(R.string.search_match_tag)
    "note" -> stringResource(R.string.search_match_note)
    "pinned" -> stringResource(R.string.search_match_pinned)
    "duplicate" -> stringResource(R.string.search_match_duplicate)
    "follow-up" -> stringResource(R.string.search_match_follow_up)
    "priority" -> stringResource(R.string.search_match_priority)
    else -> field
}
