package com.linknest.feature.dashboard

import android.graphics.Color.parseColor
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.linknest.core.designsystem.component.GlassPanel
import com.linknest.core.designsystem.component.LinkNestGradientBackground
import com.linknest.core.model.DashboardCategory
import com.linknest.core.model.DashboardSmartSection
import com.linknest.core.model.LayoutMode
import com.linknest.core.model.WebsiteListItem
import com.linknest.core.designsystem.R as DesignSystemR

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
    var smartSectionTarget by remember { mutableStateOf<DashboardSmartSection?>(null) }
    var websiteActionTarget by remember { mutableStateOf<WebsiteListItem?>(null) }
    var categoryActionTarget by remember { mutableStateOf<DashboardCategory?>(null) }
    var websiteDeleteTarget by remember { mutableStateOf<WebsiteListItem?>(null) }
    var categoryDeleteTarget by remember { mutableStateOf<DashboardCategory?>(null) }
    var categoryEditTarget by remember { mutableStateOf<DashboardCategory?>(null) }
    var categoryReorderTarget by remember { mutableStateOf<DashboardCategory?>(null) }
    var websiteReorderTarget by remember { mutableStateOf<WebsiteListItem?>(null) }

    smartSectionTarget?.let { section ->
        SmartSectionSheet(section = section, onDismiss = { smartSectionTarget = null }, onOpenWebsite = onOpenWebsite)
    }
    websiteActionTarget?.let { website ->
        WebsiteActionsSheet(
            website = website,
            categories = uiState.dashboard.categories,
            onDismiss = { websiteActionTarget = null },
            onEdit = { websiteActionTarget = null; onEditWebsite(website.id) },
            onReorderInCategory = {
                websiteActionTarget = null
                websiteReorderTarget = website
            },
            onRelocate = { targetCategoryId ->
                websiteActionTarget = null
                if (targetCategoryId != website.categoryId) onMoveWebsiteToCategory(website.id, targetCategoryId)
            },
            onPinToggle = { websiteActionTarget = null; onSetPinned(website.id, !website.isPinned) },
            onDelete = { websiteActionTarget = null; websiteDeleteTarget = website },
        )
    }
    categoryActionTarget?.let { category ->
        CategoryActionsSheet(
            category = category,
            onDismiss = { categoryActionTarget = null },
            onEdit = { categoryActionTarget = null; categoryEditTarget = category },
            onReorder = {
                categoryActionTarget = null
                categoryReorderTarget = category
            },
            onArchive = { categoryActionTarget = null; onArchiveCategory(category.id) },
            onDelete = { categoryActionTarget = null; categoryDeleteTarget = category },
        )
    }
    categoryReorderTarget?.let { category ->
        val currentIndex = uiState.dashboard.categories.indexOfFirst { it.id == category.id }
        if (currentIndex >= 0) {
            CategoryReorderSheet(
                categories = uiState.dashboard.categories,
                selectedCategoryId = category.id,
                onDismiss = { categoryReorderTarget = null },
                onMoveToIndex = { index ->
                    onMoveCategory(category.id, index - currentIndex)
                    categoryReorderTarget = null
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
                onDismiss = { websiteReorderTarget = null },
                onMoveToIndex = { index ->
                    onMoveWebsite(website.categoryId, website.id, index - currentIndex)
                    websiteReorderTarget = null
                },
            )
        }
    }
    categoryEditTarget?.let { category ->
        EditCategorySheet(
            category = category,
            onDismiss = { categoryEditTarget = null },
            onSubmit = { name, colorHex, iconValue ->
                onUpdateCategory(category.id, name, colorHex, iconValue)
                categoryEditTarget = null
            },
        )
    }
    websiteDeleteTarget?.let { website ->
        ConfirmDeleteDialog(
            title = "Delete website",
            message = "Remove ${website.title} from LinkNest?",
            onDismiss = { websiteDeleteTarget = null },
            onConfirm = { onDeleteWebsite(website.id); websiteDeleteTarget = null },
        )
    }
    categoryDeleteTarget?.let { category ->
        ConfirmDeleteDialog(
            title = "Delete category",
            message = "Delete ${category.name} and its saved websites?",
            onDismiss = { categoryDeleteTarget = null },
            onConfirm = { onDeleteCategory(category.id); categoryDeleteTarget = null },
        )
    }

    LinkNestGradientBackground(
        modifier = modifier
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
            bottomBar = {
                DashboardBottomDock(
                    recentSection = uiState.dashboard.recentSection,
                    mostUsedSection = uiState.dashboard.mostUsedSection,
                    onOpenSection = { smartSectionTarget = it },
                    onAddWebsite = onAddWebsite,
                )
            },
        ) { innerPadding ->
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(key = "dashboard-header") {
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
                        item(key = "pinned") { PinnedSection(section = pinned, onOpenWebsite = onOpenWebsite) }
                    }
                    val compactSmartSections = uiState.dashboard.smartSections.filter { section ->
                        section.id !in setOf("pinned", "recent", "most_used")
                    }
                    if (compactSmartSections.isNotEmpty()) {
                        item(key = "compact-smart-sections") {
                            CompactSmartSections(
                                sections = compactSmartSections,
                                onOpenSection = { smartSectionTarget = it },
                            )
                        }
                    }
                    items(uiState.dashboard.categories, key = { "category-${it.id}" }) { category ->
                        CategorySection(
                            category = category,
                            layoutMode = uiState.dashboard.layoutMode,
                            adaptiveMinTileWidth = uiState.dashboard.tileSizeDp.coerceIn(144, 196),
                            focusedCategoryId = uiState.focusedCategoryId,
                            focusedWebsiteId = uiState.focusedWebsiteId,
                            onToggleCategory = { onToggleCategory(category.id) },
                            onCategoryLongPress = { categoryActionTarget = category },
                            onOpenWebsite = onOpenWebsite,
                            onWebsiteLongPress = { websiteActionTarget = it },
                        )
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
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth < 360.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardBranding()
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
                    DashboardBranding(modifier = Modifier.weight(1f))
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
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .focusRequester(queryFocusRequester),
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Rounded.ManageSearch,
                    contentDescription = null,
                )
            },
            placeholder = { Text("Search websites, tags, or domains") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
            ),
        )
    }
}

@Composable
private fun DashboardBranding(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = DesignSystemR.drawable.ic_linknest_mark),
                contentDescription = "LinkNest",
                tint = Color.Unspecified,
                modifier = Modifier.size(28.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "LinkNest",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Search, organize, and open fast",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
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
            Icon(Icons.AutoMirrored.Rounded.ManageSearch, contentDescription = "Open advanced search")
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
        }
    }
}

@Composable
private fun LayoutToggle(layoutMode: LayoutMode, onLayoutModeChanged: (LayoutMode) -> Unit) {
    GlassPanel {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onLayoutModeChanged(LayoutMode.LIST) }) {
                Icon(Icons.Rounded.ViewAgenda, contentDescription = "List mode", tint = if (layoutMode == LayoutMode.LIST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { onLayoutModeChanged(LayoutMode.GRID) }) {
                Icon(Icons.Rounded.GridView, contentDescription = "Grid mode", tint = if (layoutMode == LayoutMode.GRID) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DashboardBottomDock(
    recentSection: DashboardSmartSection?,
    mostUsedSection: DashboardSmartSection?,
    onOpenSection: (DashboardSmartSection) -> Unit,
    onAddWebsite: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < 420.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DashboardQuickActions(
                            recentSection = recentSection,
                            mostUsedSection = mostUsedSection,
                            onOpenSection = onOpenSection,
                        )
                        Button(
                            onClick = onAddWebsite,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Website")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DashboardQuickActions(
                            modifier = Modifier.weight(1f),
                            recentSection = recentSection,
                            mostUsedSection = mostUsedSection,
                            onOpenSection = onOpenSection,
                        )
                        FloatingActionButton(
                            onClick = onAddWebsite,
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add Website")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DashboardQuickActions(
    recentSection: DashboardSmartSection?,
    mostUsedSection: DashboardSmartSection?,
    onOpenSection: (DashboardSmartSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        recentSection?.let { section ->
            FilledTonalButton(
                onClick = { onOpenSection(section) },
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Rounded.History, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Recent")
            }
        }
        mostUsedSection?.let { section ->
            FilledTonalButton(
                onClick = { onOpenSection(section) },
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Frequent")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactSmartSections(
    sections: List<DashboardSmartSection>,
    onOpenSection: (DashboardSmartSection) -> Unit,
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Smart Sections",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            sections.forEach { section ->
                AssistChip(
                    onClick = { onOpenSection(section) },
                    label = { Text("${section.title} (${section.websites.size})") },
                    leadingIcon = {
                        Icon(
                            imageVector = when (section.id) {
                                "needs_attention" -> Icons.Rounded.Tune
                                "duplicates" -> Icons.AutoMirrored.Rounded.ManageSearch
                                "unsorted" -> Icons.AutoMirrored.Rounded.Label
                                "last_imported" -> Icons.Rounded.History
                                else -> Icons.Rounded.Tune
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
private fun PinnedSection(section: DashboardSmartSection, onOpenWebsite: (WebsiteListItem) -> Unit) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Text("Pinned", fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            section.websites.forEach { website ->
                AssistChip(
                    onClick = { onOpenWebsite(website) },
                    label = { Text(website.title) },
                    leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySection(
    category: DashboardCategory,
    layoutMode: LayoutMode,
    adaptiveMinTileWidth: Int,
    focusedCategoryId: Long?,
    focusedWebsiteId: Long?,
    onToggleCategory: () -> Unit,
    onCategoryLongPress: () -> Unit,
    onOpenWebsite: (WebsiteListItem) -> Unit,
    onWebsiteLongPress: (WebsiteListItem) -> Unit,
) {
    val accentColor = category.colorHex.toColorOr(MaterialTheme.colorScheme.primary)
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .background(Color.Transparent, RoundedCornerShape(24.dp)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onToggleCategory, onLongClick = onCategoryLongPress),
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
                    Text("${category.websiteCount} websites", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (focusedCategoryId == category.id) {
                Icon(Icons.Rounded.PushPin, contentDescription = null, tint = accentColor)
            }
        }

        AnimatedVisibility(visible = !category.isCollapsed) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                if (layoutMode == LayoutMode.LIST) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        category.websites.forEach { website ->
                            WebsiteTile(
                                website = website,
                                accentColor = accentColor,
                                modifier = Modifier.fillMaxWidth(),
                                isFocused = focusedWebsiteId == website.id,
                                onOpenWebsite = { onOpenWebsite(website) },
                                onLongPress = { onWebsiteLongPress(website) },
                            )
                        }
                    }
                } else {
                    BoxWithConstraints {
                        val spacing = 12.dp
                        val columns = (maxWidth / adaptiveMinTileWidth.dp).toInt().coerceAtLeast(1)
                        val tileWidth = ((maxWidth - spacing * (columns - 1)) / columns).coerceAtLeast(144.dp)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(spacing),
                            verticalArrangement = Arrangement.spacedBy(spacing),
                            maxItemsInEachRow = columns,
                        ) {
                            category.websites.forEach { website ->
                                WebsiteTile(
                                    website = website,
                                    accentColor = accentColor,
                                    modifier = Modifier.width(tileWidth),
                                    layoutMode = layoutMode,
                                    isFocused = focusedWebsiteId == website.id,
                                    onOpenWebsite = { onOpenWebsite(website) },
                                    onLongPress = { onWebsiteLongPress(website) },
                                )
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
private fun WebsiteTile(
    website: WebsiteListItem,
    accentColor: Color,
    modifier: Modifier,
    layoutMode: LayoutMode = LayoutMode.LIST,
    isFocused: Boolean,
    onOpenWebsite: () -> Unit,
    onLongPress: () -> Unit,
) {
    if (layoutMode == LayoutMode.GRID) {
        GridWebsiteTile(
            website = website,
            accentColor = accentColor,
            modifier = modifier,
            isFocused = isFocused,
            onOpenWebsite = onOpenWebsite,
            onLongPress = onLongPress,
        )
    } else {
        ListWebsiteTile(
            website = website,
            accentColor = accentColor,
            modifier = modifier,
            isFocused = isFocused,
            onOpenWebsite = onOpenWebsite,
            onLongPress = onLongPress,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListWebsiteTile(
    website: WebsiteListItem,
    accentColor: Color,
    modifier: Modifier,
    isFocused: Boolean,
    onOpenWebsite: () -> Unit,
    onLongPress: () -> Unit,
) {
    val metadataSummary = website.dashboardMetadataSummary()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isFocused) {
                    accentColor.copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f)
                },
            )
            .combinedClickable(onClick = onOpenWebsite, onLongClick = onLongPress)
            .padding(14.dp),
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
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(healthStatusColor(website.healthStatus)))
                Text(website.healthStatus.name.lowercase().replaceFirstChar(Char::uppercaseChar), style = MaterialTheme.typography.labelSmall)
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
    modifier: Modifier,
    isFocused: Boolean,
    onOpenWebsite: () -> Unit,
    onLongPress: () -> Unit,
) {
    val metadataSummary = website.dashboardMetadataSummary()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isFocused) {
                    accentColor.copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                },
            )
            .combinedClickable(onClick = onOpenWebsite, onLongClick = onLongPress)
            .padding(14.dp)
            .animateContentSize(),
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
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(healthStatusColor(website.healthStatus)))
            Text(
                text = website.healthStatus.name.lowercase().replaceFirstChar(Char::uppercaseChar),
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
                contentDescription = null,
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
    when {
        !iconUrl.isNullOrBlank() -> AsyncImage(
            model = iconUrl,
            contentDescription = null,
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
        ) { Text(title.take(1).uppercase(), fontWeight = FontWeight.Bold) }
    }
}

private fun String.toColorOr(fallback: Color): Color = runCatching { Color(parseColor(this)) }.getOrDefault(fallback)
