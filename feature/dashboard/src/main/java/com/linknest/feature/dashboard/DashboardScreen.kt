package com.linknest.feature.dashboard

import androidx.core.graphics.toColorInt
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage

import com.linknest.core.designsystem.labelResource
import com.linknest.core.model.DashboardCategory
import com.linknest.core.model.DashboardSmartSection
import com.linknest.core.model.LayoutMode
import com.linknest.core.model.WebsiteListItem

@Composable
fun DashboardRoute(
    onAddWebsite: () -> Unit,
    onEditWebsite: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHealthReport: () -> Unit,
    onOpenWebsite: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onMessageConsumed()
        }
    }

    DashboardScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        onAddWebsite = onAddWebsite,
        onEditWebsite = onEditWebsite,
        onOpenSearch = onOpenSearch,
        onOpenSettings = onOpenSettings,
        onOpenHealthReport = onOpenHealthReport,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onLayoutModeChanged = viewModel::onLayoutModeChanged,
        onToggleCategory = viewModel::onToggleCategoryCollapsed,
        onOpenWebsite = { website ->
            viewModel.onWebsiteOpened(website.id)
            onOpenWebsite(website.id, website.normalizedUrl)
        },
        onMoveCategory = viewModel::onMoveCategory,
        onMoveWebsite = viewModel::onMoveWebsite,
        onMoveWebsiteToCategory = viewModel::onMoveWebsiteToCategory,
        onDeleteWebsite = viewModel::onDeleteWebsite,
        onSetPinned = viewModel::onSetPinned,
        onUpdateCategory = viewModel::onUpdateCategory,
        onArchiveCategory = viewModel::onArchiveCategory,
        onDeleteCategory = viewModel::onDeleteCategory,
    )
}

@Composable
private fun DashboardScreen(
    uiState: DashboardUiState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onAddWebsite: () -> Unit,
    onEditWebsite: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHealthReport: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onLayoutModeChanged: (LayoutMode) -> Unit,
    onToggleCategory: (Long) -> Unit,
    onOpenWebsite: (WebsiteListItem) -> Unit,
    onMoveCategory: (Long, Int) -> Unit,
    onMoveWebsite: (Long, Long, Int) -> Unit,
    onMoveWebsiteToCategory: (Long, Long) -> Unit,
    onDeleteWebsite: (Long) -> Unit,
    onSetPinned: (Long, Boolean) -> Unit,
    onUpdateCategory: (Long, String, String, String?) -> Unit,
    onArchiveCategory: (Long) -> Unit,
    onDeleteCategory: (Long) -> Unit,
) {
    val queryFocusRequester = remember { FocusRequester() }
    var websiteActionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var categoryActionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var websiteDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var categoryDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var categoryEditId by rememberSaveable { mutableStateOf<Long?>(null) }
    var categoryReorderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var websiteReorderId by rememberSaveable { mutableStateOf<Long?>(null) }
    val websiteActionTarget = websiteActionId?.let { id ->
        uiState.dashboard.categories.firstNotNullOfOrNull { category -> category.websites.firstOrNull { it.id == id } }
    }
    val categoryActionTarget = categoryActionId?.let { id -> uiState.dashboard.categories.firstOrNull { it.id == id } }
    val websiteDeleteTarget = websiteDeleteId?.let { id ->
        uiState.dashboard.categories.firstNotNullOfOrNull { category -> category.websites.firstOrNull { it.id == id } }
    }
    val categoryDeleteTarget = categoryDeleteId?.let { id -> uiState.dashboard.categories.firstOrNull { it.id == id } }
    val categoryEditTarget = categoryEditId?.let { id -> uiState.dashboard.categories.firstOrNull { it.id == id } }
    val categoryReorderTarget = categoryReorderId?.let { id -> uiState.dashboard.categories.firstOrNull { it.id == id } }
    val websiteReorderTarget = websiteReorderId?.let { id ->
        uiState.dashboard.categories.firstNotNullOfOrNull { category -> category.websites.firstOrNull { it.id == id } }
    }

    val resources = LocalResources.current
    LaunchedEffect(uiState.isLoading, uiState.dashboard.categories) {
        if (!uiState.isLoading) {
            val missing = (websiteActionId != null && websiteActionTarget == null) ||
                (categoryActionId != null && categoryActionTarget == null) ||
                (websiteDeleteId != null && websiteDeleteTarget == null) ||
                (categoryDeleteId != null && categoryDeleteTarget == null) ||
                (categoryEditId != null && categoryEditTarget == null) ||
                (categoryReorderId != null && categoryReorderTarget == null) ||
                (websiteReorderId != null && websiteReorderTarget == null)
            if (missing) {
                if (websiteActionTarget == null) websiteActionId = null
                if (categoryActionTarget == null) categoryActionId = null
                if (websiteDeleteTarget == null) websiteDeleteId = null
                if (categoryDeleteTarget == null) categoryDeleteId = null
                if (categoryEditTarget == null) categoryEditId = null
                if (categoryReorderTarget == null) categoryReorderId = null
                if (websiteReorderTarget == null) websiteReorderId = null
                snackbarHostState.showSnackbar(resources.getString(R.string.dashboard_selection_unavailable))
            }
        }
    }

    websiteActionTarget?.let { website ->
        WebsiteActionsSheet(
            website = website,
            categories = uiState.dashboard.categories,
            onDismiss = { websiteActionId = null },
            onEdit = { websiteActionId = null; onEditWebsite(website.id) },
            onReorderInCategory = {
                websiteActionId = null
                websiteReorderId = website.id
            },
            onRelocate = { targetCategoryId ->
                websiteActionId = null
                if (targetCategoryId != website.categoryId) onMoveWebsiteToCategory(website.id, targetCategoryId)
            },
            onPinToggle = { websiteActionId = null; onSetPinned(website.id, !website.isPinned) },
            onDelete = { websiteActionId = null; websiteDeleteId = website.id },
        )
    }
    categoryActionTarget?.let { category ->
        CategoryActionsSheet(
            category = category,
            onDismiss = { categoryActionId = null },
            onEdit = { categoryActionId = null; categoryEditId = category.id },
            onReorder = {
                categoryActionId = null
                categoryReorderId = category.id
            },
            onArchive = { categoryActionId = null; onArchiveCategory(category.id) },
            onDelete = { categoryActionId = null; categoryDeleteId = category.id },
        )
    }
    categoryReorderTarget?.let { category ->
        val currentIndex = uiState.dashboard.categories.indexOfFirst { it.id == category.id }
        if (currentIndex >= 0) {
            CategoryReorderSheet(
                categories = uiState.dashboard.categories,
                selectedCategoryId = category.id,
                onDismiss = { categoryReorderId = null },
                onMoveToIndex = { index ->
                    onMoveCategory(category.id, index - currentIndex)
                    categoryReorderId = null
                },
            )
        }
    }
    websiteReorderTarget?.let { website ->
        val category = uiState.dashboard.categories.firstOrNull { it.id == website.categoryId }
        val currentIndex = category?.websites?.indexOfFirst { it.id == website.id } ?: -1
        if (category != null && currentIndex >= 0) {
            WebsiteReorderSheet(
                category = category,
                selectedWebsiteId = website.id,
                onDismiss = { websiteReorderId = null },
                onMoveToIndex = { index ->
                    onMoveWebsite(website.categoryId, website.id, index - currentIndex)
                    websiteReorderId = null
                },
            )
        }
    }
    categoryEditTarget?.let { category ->
        EditCategorySheet(
            category = category,
            onDismiss = { categoryEditId = null },
            onSubmit = { name, colorHex, iconValue ->
                onUpdateCategory(category.id, name, colorHex, iconValue)
                categoryEditId = null
            },
        )
    }
    websiteDeleteTarget?.let { website ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.dashboard_delete_website),
            message = stringResource(R.string.dashboard_confirm_delete_website, website.title),
            onDismiss = { websiteDeleteId = null },
            onConfirm = { onDeleteWebsite(website.id); websiteDeleteId = null },
        )
    }
    categoryDeleteTarget?.let { category ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.dashboard_delete_category),
            message = stringResource(R.string.dashboard_confirm_delete_category, category.name),
            onDismiss = { categoryDeleteId = null },
            onConfirm = { onDeleteCategory(category.id); categoryDeleteId = null },
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                when {
                    event.isCtrlPressed && event.key == Key.K -> {
                        onOpenSearch()
                        true
                    }
                    event.isCtrlPressed && event.key == Key.L -> {
                        queryFocusRequester.requestFocus()
                        true
                    }
                    event.isCtrlPressed && event.key == Key.N -> {
                        onAddWebsite()
                        true
                    }
                    event.isCtrlPressed && event.key == Key.G -> {
                        onLayoutModeChanged(
                            if (uiState.dashboard.layoutMode == LayoutMode.GRID) LayoutMode.LIST else LayoutMode.GRID,
                        )
                        true
                    }
                    event.isCtrlPressed && event.key == Key.H -> {
                        onOpenHealthReport()
                        true
                    }
                    else -> false
                }
            },
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddWebsite,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.dashboard_add_website))
                }
            },
        ) { innerPadding ->
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    val loadingLabel = stringResource(R.string.dashboard_loading)
                    CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loadingLabel })
                }
            } else {
                val columns = if (uiState.dashboard.layoutMode == LayoutMode.GRID) {
                    GridCells.Adaptive(uiState.dashboard.adaptiveGridMinSizeDp.dp)
                } else GridCells.Fixed(1)
                val fallbackAccent = MaterialTheme.colorScheme.primary
                LazyVerticalGrid(
                    columns = columns,
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "dashboard-header", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                        DashboardHeader(
                            searchQuery = uiState.searchQuery,
                            layoutMode = uiState.dashboard.layoutMode,
                            queryFocusRequester = queryFocusRequester,
                            onSearchQueryChanged = onSearchQueryChanged,
                            onLayoutModeChanged = onLayoutModeChanged,
                            onOpenSearch = onOpenSearch,
                            onOpenSettings = onOpenSettings,
                        )
                    }
                    uiState.dashboard.pinnedSection?.takeIf { it.websites.isNotEmpty() }?.let { pinned ->
                        item(key = "pinned", span = { GridItemSpan(maxLineSpan) }, contentType = "pinned") {
                            PinnedSection(section = pinned, onOpenWebsite = onOpenWebsite)
                        }
                    }
                    if (uiState.dashboard.categories.isEmpty()) {
                        item(key = "empty", span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
                            Text(stringResource(R.string.dashboard_no_matches))
                        }
                    }
                    uiState.dashboard.categories.forEach { category ->
                        val accentColor = category.colorHex.toColorOr(fallbackAccent)
                        item(key = "category-${category.id}", span = { GridItemSpan(maxLineSpan) }, contentType = "category") {
                            CategoryHeader(
                                category = category,
                                accentColor = accentColor,
                                focusedCategoryId = uiState.focusedCategoryId,
                                onToggleCategory = { onToggleCategory(category.id) },
                                onCategoryLongPress = { categoryActionId = category.id },
                            )
                        }
                        if (!category.isCollapsed) {
                            items(category.websites, key = { it.id }, contentType = { uiState.dashboard.layoutMode }) { website ->
                                WebsiteTile(
                                    website = website,
                                    accentColor = accentColor,
                                    modifier = Modifier.fillMaxWidth(),
                                    layoutMode = uiState.dashboard.layoutMode,
                                    isFocused = uiState.focusedWebsiteId == website.id,
                                    onOpenWebsite = { onOpenWebsite(website) },
                                    onLongPress = { websiteActionId = website.id },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    searchQuery: String,
    layoutMode: LayoutMode,
    queryFocusRequester: FocusRequester,
    onSearchQueryChanged: (String) -> Unit,
    onLayoutModeChanged: (LayoutMode) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            if (maxWidth < 420.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DashboardSearchField(
                        searchQuery = searchQuery,
                        queryFocusRequester = queryFocusRequester,
                        onSearchQueryChanged = onSearchQueryChanged,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LayoutToggle(
                            layoutMode = layoutMode,
                            onLayoutModeChanged = onLayoutModeChanged,
                        )
                        DashboardHeaderActions(
                            onOpenSearch = onOpenSearch,
                            onOpenSettings = onOpenSettings,
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DashboardSearchField(
                        searchQuery = searchQuery,
                        queryFocusRequester = queryFocusRequester,
                        onSearchQueryChanged = onSearchQueryChanged,
                        modifier = Modifier.weight(1f),
                    )
                    LayoutToggle(
                        layoutMode = layoutMode,
                        onLayoutModeChanged = onLayoutModeChanged,
                    )
                    DashboardHeaderActions(
                        onOpenSearch = onOpenSearch,
                        onOpenSettings = onOpenSettings,
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardSearchField(
    searchQuery: String,
    queryFocusRequester: FocusRequester,
    onSearchQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .focusRequester(queryFocusRequester),
        leadingIcon = {
            Icon(
                Icons.AutoMirrored.Rounded.ManageSearch,
                contentDescription = stringResource(R.string.dashboard_search),
            )
        },
        placeholder = { Text(stringResource(R.string.dashboard_search_hint)) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
        ),
    )
}

@Composable
private fun DashboardHeaderActions(
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onOpenSearch) {
            Icon(Icons.AutoMirrored.Rounded.ManageSearch, contentDescription = stringResource(R.string.dashboard_open_search))
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.dashboard_settings))
        }
    }
}

@Composable
private fun LayoutToggle(layoutMode: LayoutMode, onLayoutModeChanged: (LayoutMode) -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(modifier = Modifier.semantics { selected = layoutMode == LayoutMode.LIST }, onClick = { onLayoutModeChanged(LayoutMode.LIST) }) {
                Icon(Icons.Rounded.ViewAgenda, contentDescription = stringResource(R.string.dashboard_list_mode), tint = if (layoutMode == LayoutMode.LIST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(modifier = Modifier.semantics { selected = layoutMode == LayoutMode.GRID }, onClick = { onLayoutModeChanged(LayoutMode.GRID) }) {
                Icon(Icons.Rounded.GridView, contentDescription = stringResource(R.string.dashboard_grid_mode), tint = if (layoutMode == LayoutMode.GRID) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PinnedSection(section: DashboardSmartSection, onOpenWebsite: (WebsiteListItem) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(stringResource(R.string.dashboard_pinned), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                section.websites.forEach { website ->
                    AssistChip(
                        onClick = { onOpenWebsite(website) },
                        label = { Text(website.title) },
                        leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = stringResource(R.string.dashboard_pinned)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(
    category: DashboardCategory,
    accentColor: Color,
    focusedCategoryId: Long?,
    onToggleCategory: () -> Unit,
    onCategoryLongPress: () -> Unit,
) {
    val expandedState = stringResource(if (category.isCollapsed) R.string.dashboard_collapsed else R.string.dashboard_expanded)
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .semantics { stateDescription = expandedState }
                    .combinedClickable(
                        onClick = onToggleCategory,
                        onClickLabel = stringResource(if (category.isCollapsed) R.string.dashboard_expand_category else R.string.dashboard_collapse_category),
                        onLongClick = onCategoryLongPress,
                        onLongClickLabel = stringResource(R.string.dashboard_category_actions),
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(category.iconValue ?: category.name.take(1))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(category.name, fontWeight = FontWeight.SemiBold)
                        Text(pluralStringResource(R.plurals.dashboard_website_count, category.websiteCount, category.websiteCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (focusedCategoryId == category.id) {
                    Icon(Icons.Rounded.PushPin, contentDescription = stringResource(R.string.dashboard_focused_category), tint = accentColor)
                }
            }

        }
    }
}

@Composable
private fun WebsiteTile(
    website: WebsiteListItem,
    accentColor: Color,
    modifier: Modifier,
    layoutMode: LayoutMode,
    isFocused: Boolean,
    onOpenWebsite: () -> Unit,
    onLongPress: () -> Unit,
) {
    val grid = layoutMode == LayoutMode.GRID
    val resources = LocalResources.current
    val healthLabel = stringResource(website.healthStatus.labelResource)
    val backgroundColor = if (isFocused) {
        accentColor.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (grid) 0.42f else 0.46f)
    }
    val tile = modifier
        .clip(RoundedCornerShape(if (grid) 20.dp else 18.dp))
        .background(backgroundColor)
        .semantics {
            contentDescription = resources.getString(
                if (website.isPinned) R.string.dashboard_tile_description_pinned else R.string.dashboard_tile_description,
                website.title, website.domain, healthLabel,
            )
        }
        .combinedClickable(
            onClickLabel = stringResource(R.string.dashboard_open_website, website.title),
            onLongClickLabel = stringResource(R.string.dashboard_website_actions),
            role = Role.Button,
            onClick = onOpenWebsite,
            onLongClick = onLongPress,
        )
        .padding(14.dp)
    val metadataSummary = website.dashboardMetadataSummary(resources)
    if (grid) {
        GridWebsiteTile(website, accentColor, metadataSummary, healthLabel, tile)
    } else {
        ListWebsiteTile(website, accentColor, metadataSummary, healthLabel, tile)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListWebsiteTile(
    website: WebsiteListItem,
    accentColor: Color,
    metadataSummary: String?,
    healthLabel: String,
    modifier: Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WebsiteIcon(title = website.title, emoji = website.emojiIcon, iconUrl = website.preferredIconUrl, accentColor = accentColor)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    website.title,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (website.isPinned) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Rounded.PushPin, contentDescription = null, modifier = Modifier.size(14.dp), tint = accentColor)
                }
            }
            Text(
                website.domain,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            metadataSummary?.let { summary ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Spacer(modifier = Modifier.size(8.dp).background(healthStatusColor(website.healthStatus), CircleShape))
                Text(healthLabel, style = MaterialTheme.typography.labelSmall)
            }
            if (website.tagNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    website.tagNames.take(3).forEach { tag ->
                        MiniTagChip(tag = tag)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GridWebsiteTile(
    website: WebsiteListItem,
    accentColor: Color,
    metadataSummary: String?,
    healthLabel: String,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WebsiteIcon(title = website.title, emoji = website.emojiIcon, iconUrl = website.preferredIconUrl, accentColor = accentColor)
            if (website.isPinned) {
                Icon(Icons.Rounded.PushPin, contentDescription = null, modifier = Modifier.size(16.dp), tint = accentColor)
            }
        }
        Text(
            text = website.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = website.domain,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        metadataSummary?.let { summary ->
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Spacer(modifier = Modifier.size(8.dp).background(healthStatusColor(website.healthStatus), CircleShape))
            Text(
                text = healthLabel,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (website.tagNames.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                website.tagNames.take(2).forEach { tag ->
                    MiniTagChip(tag = tag)
                }
            }
        }
    }
}

@Composable
private fun MiniTagChip(tag: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.52f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.Label,
                contentDescription = stringResource(R.string.dashboard_tag),
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = tag,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WebsiteIcon(title: String, emoji: String?, iconUrl: String?, accentColor: Color) {
    var failed by remember(iconUrl) { mutableStateOf(false) }
    when {
        !iconUrl.isNullOrBlank() && !failed -> AsyncImage(
            model = iconUrl,
            onError = { failed = true },
            contentDescription = stringResource(R.string.dashboard_website_icon, title),
            modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
            contentScale = ContentScale.Crop,
        )
        !emoji.isNullOrBlank() -> Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) { Text(emoji) }
        else -> Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) { Text(title.take(1).uppercase(LocalResources.current.configuration.locales[0]), fontWeight = FontWeight.Bold) }
    }
}

private fun String.toColorOr(fallback: Color): Color = runCatching { Color(this.toColorInt()) }.getOrDefault(fallback)
