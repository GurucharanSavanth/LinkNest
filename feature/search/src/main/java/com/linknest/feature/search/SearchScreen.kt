package com.linknest.feature.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Search & Collections") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Go back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { filtersExpanded = !filtersExpanded }) {
                            Icon(Icons.Rounded.FilterAlt, contentDescription = "Toggle filters")
                        }
                    },
                )
            },
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                val showFilterBuilder = filtersExpanded || maxWidth >= 920.dp
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
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
                                    label = { Text("Offline search") },
                                    placeholder = { Text("Search titles, domains, categories, tags, and notes") },
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
                                        "Use smart collections, recent queries, or saved searches to reopen focused slices of your library."
                                    } else {
                                        "No websites matched the current search and filter combination."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    items(
                        items = uiState.results.groups,
                        key = { group -> group.title },
                    ) { group ->
                        SearchGroup(group = group, onOpenWebsite = onOpenWebsite)
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
            text = "Smart Collections",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SearchSmartCollection.entries.forEach { collection ->
                FilterChip(
                    selected = activeCollection == collection,
                    onClick = { onApplySmartCollection(collection) },
                    label = { Text(collection.label) },
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
                text = "Saved Searches",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedButton(onClick = onSaveCurrentFilter) {
                Text("Save Search")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (savedFilters.isEmpty()) {
            Text(
                text = "No saved searches yet.",
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
                            Icon(
                                imageVector = Icons.Rounded.ClearAll,
                                contentDescription = "Delete filter",
                                modifier = Modifier.clickable { onDeleteSavedFilter(filter.id) },
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
                    text = "Recent Queries",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = onClearRecentQueries) {
                    Text("Clear History")
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
                text = "Suggestions",
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
                text = "Filter Builder",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = onClearFilters, enabled = uiState.hasActiveFilters) {
                Text("Clear")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        FilterSection(
            title = "Quick Flags",
            content = {
                ToggleChip("Pinned", uiState.pinnedOnly) { onPinnedOnlyChanged(!uiState.pinnedOnly) }
                ToggleChip("Recent", uiState.recentOnly) { onRecentOnlyChanged(!uiState.recentOnly) }
                ToggleChip("Duplicates", uiState.duplicatesOnly) { onDuplicatesOnlyChanged(!uiState.duplicatesOnly) }
                ToggleChip("Needs Attention", uiState.includeNeedsAttention) { onNeedsAttentionChanged(!uiState.includeNeedsAttention) }
            },
        )
        FilterSection(
            title = "Categories",
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
            title = "Tags",
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
            title = "Health",
            content = {
                HealthStatus.entries.forEach { status ->
                    if (status != HealthStatus.UNKNOWN) {
                        ToggleChip(
                            label = status.label,
                            selected = status in uiState.selectedHealthStatuses,
                        ) { onToggleHealthStatus(status) }
                    }
                }
            },
        )
        FilterSection(
            title = "Priority",
            content = {
                WebsitePriority.entries.forEach { priority ->
                    ToggleChip(
                        label = priority.label,
                        selected = priority in uiState.selectedPriorities,
                    ) { onTogglePriority(priority) }
                }
            },
        )
        FilterSection(
            title = "Follow-up",
            content = {
                FollowUpStatus.entries.forEach { status ->
                    if (status != FollowUpStatus.NONE) {
                        ToggleChip(
                            label = status.label,
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
                                text = "Matched: ${item.matchedFields.joinToString()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                    Text(
                        text = item.healthStatus.label,
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
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save current filter") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Filter name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private val HealthStatus.label: String
    get() = when (this) {
        HealthStatus.OK -> "Good"
        HealthStatus.LOGIN_REQUIRED -> "Login"
        HealthStatus.BLOCKED -> "Blocked"
        HealthStatus.REDIRECTED -> "Redirected"
        HealthStatus.DNS_FAILED -> "DNS"
        HealthStatus.SSL_ISSUE -> "TLS"
        HealthStatus.DEAD -> "Dead"
        HealthStatus.TIMEOUT -> "Timeout"
        HealthStatus.UNKNOWN -> "Unknown"
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

private val WebsitePriority.label: String
    get() = when (this) {
        WebsitePriority.LOW -> "Low"
        WebsitePriority.NORMAL -> "Normal"
        WebsitePriority.HIGH -> "High"
        WebsitePriority.CRITICAL -> "Critical"
    }

private val FollowUpStatus.label: String
    get() = when (this) {
        FollowUpStatus.NONE -> "None"
        FollowUpStatus.REVIEW -> "Review"
        FollowUpStatus.IN_PROGRESS -> "In Progress"
        FollowUpStatus.WAITING -> "Waiting"
        FollowUpStatus.DONE -> "Done"
    }
