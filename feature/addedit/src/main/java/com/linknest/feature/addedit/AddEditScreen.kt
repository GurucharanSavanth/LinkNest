package com.linknest.feature.addedit

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.linknest.core.designsystem.component.GlassPanel
import com.linknest.core.designsystem.component.LinkNestGradientBackground
import com.linknest.core.model.AddWebsitePhase
import com.linknest.core.model.CategoryDraft
import com.linknest.core.model.DuplicateCheckResult
import com.linknest.core.model.DuplicateDecision
import com.linknest.core.model.DuplicateMatchType
import com.linknest.core.model.FollowUpStatus
import com.linknest.core.model.IconType
import com.linknest.core.model.MetadataPreview
import com.linknest.core.model.SelectableCategory
import com.linknest.core.model.SmartCaptureResult
import com.linknest.core.model.TagModel
import com.linknest.core.model.WebsitePriority

private val CategoryColors = listOf(
    "#7C4DFF",
    "#00BCD4",
    "#4CAF50",
    "#FF9800",
    "#F06292",
    "#FFD54F",
)
private val CategoryIcons = listOf("🧠", "📁", "🚀", "🎨", "💼", "🌐")

@Composable
fun AddEditRoute(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDiscardChangesDialog by remember { mutableStateOf(false) }

    val requestBackNavigation = {
        if (uiState.hasUnsavedChanges && !uiState.isSubmitting) {
            showDiscardChangesDialog = true
        } else {
            onDone()
        }
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onMessageConsumed()
        }
    }

    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            viewModel.onSaveCompletedConsumed()
            onDone()
        }
    }

    BackHandler(enabled = uiState.hasUnsavedChanges && !uiState.isSubmitting) {
        showDiscardChangesDialog = true
    }

    PredictiveBackHandler(enabled = uiState.hasUnsavedChanges && !uiState.isSubmitting) { backEventFlow ->
        backEventFlow.collect { }
        showDiscardChangesDialog = true
    }

    AddEditScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        onBack = requestBackNavigation,
        onUrlChanged = viewModel::onUrlChanged,
        onCategorySelected = viewModel::onCategorySelected,
        onUseSuggestedCategory = viewModel::onUseSuggestedCategory,
        onCreateCategory = viewModel::onCreateCategory,
        onTagDraftChanged = viewModel::onTagDraftChanged,
        onSelectTag = viewModel::onSelectTag,
        onCreateTag = viewModel::onCreateTag,
        onRemoveTag = viewModel::onRemoveTag,
        onNoteChanged = viewModel::onNoteChanged,
        onReasonSavedChanged = viewModel::onReasonSavedChanged,
        onSourceLabelChanged = viewModel::onSourceLabelChanged,
        onCustomLabelChanged = viewModel::onCustomLabelChanged,
        onPrioritySelected = viewModel::onPrioritySelected,
        onFollowUpStatusSelected = viewModel::onFollowUpStatusSelected,
        onRevisitDateChanged = viewModel::onRevisitDateChanged,
        onSubmit = viewModel::onSubmit,
        onResolveDuplicate = viewModel::onResolveDuplicate,
    )

    if (showDiscardChangesDialog) {
        DiscardChangesDialog(
            onDismiss = { showDiscardChangesDialog = false },
            onDiscardChanges = {
                showDiscardChangesDialog = false
                onDone()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddEditScreen(
    uiState: AddEditUiState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onUrlChanged: (String) -> Unit,
    onCategorySelected: (Long) -> Unit,
    onUseSuggestedCategory: () -> Unit,
    onCreateCategory: (CategoryDraft) -> Unit,
    onTagDraftChanged: (String) -> Unit,
    onSelectTag: (String) -> Unit,
    onCreateTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onReasonSavedChanged: (String) -> Unit,
    onSourceLabelChanged: (String) -> Unit,
    onCustomLabelChanged: (String) -> Unit,
    onPrioritySelected: (WebsitePriority) -> Unit,
    onFollowUpStatusSelected: (FollowUpStatus) -> Unit,
    onRevisitDateChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onResolveDuplicate: (DuplicateDecision) -> Unit,
) {
    var categorySheetOpen by remember { mutableStateOf(false) }

    if (categorySheetOpen) {
        CreateCategorySheet(
            onDismiss = { categorySheetOpen = false },
            onCreateCategory = { draft ->
                onCreateCategory(draft)
                categorySheetOpen = false
            },
        )
    }

    LinkNestGradientBackground(modifier = modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(if (uiState.isEditMode) "Edit Website" else "Add Website") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Go back")
                        }
                    },
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    GlassPanel {
                        Text("Secure URL intake", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Only HTTP and HTTPS websites are accepted. Local, loopback, private-network, and unsafe schemes are blocked by default.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = uiState.rawUrl,
                            onValueChange = onUrlChanged,
                            label = { Text("Website URL") },
                            leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        uiState.phaseLabel?.let { phaseLabel ->
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(phaseLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                item {
                    GlassPanel {
                        Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(12.dp))
                        CategoryPicker(
                            categories = uiState.categories,
                            selectedCategoryId = uiState.selectedCategoryId,
                            onCategorySelected = onCategorySelected,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            TextButton(onClick = { categorySheetOpen = true }) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Create category")
                            }
                            uiState.suggestedCategory?.let { suggestion ->
                                AssistChip(
                                    onClick = onUseSuggestedCategory,
                                    label = { Text("Suggested: ${suggestion.categoryName}") },
                                )
                            }
                        }
                    }
                }

                uiState.smartCapture?.let { smartCapture ->
                    item {
                        SmartCaptureCard(
                            smartCapture = smartCapture,
                            onSelectSuggestedTag = onSelectTag,
                            alreadySelectedTags = uiState.selectedTags,
                        )
                    }
                }

                if (uiState.awaitingDuplicateDecision && uiState.duplicateCheck != null) {
                    item {
                        DuplicateReviewCard(
                            duplicateCheck = uiState.duplicateCheck,
                            onResolveDuplicate = onResolveDuplicate,
                        )
                    }
                }

                item {
                    GlassPanel {
                        Text("Tags", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = uiState.tagDraft,
                            onValueChange = onTagDraftChanged,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            label = { Text("Search or create tags") },
                            singleLine = true,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (uiState.selectedTags.isNotEmpty()) {
                            Text("Selected tags", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                uiState.selectedTags.forEach { tag ->
                                    FilterChip(
                                        selected = true,
                                        onClick = { onRemoveTag(tag) },
                                        label = { Text(tag) },
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        val visibleExistingTags = if (uiState.tagSuggestions.isNotEmpty()) {
                            uiState.tagSuggestions
                        } else {
                            uiState.availableTags
                                .filterNot { tag -> uiState.selectedTags.any { it.equals(tag.name, ignoreCase = true) } }
                                .take(12)
                        }
                        if (visibleExistingTags.isNotEmpty()) {
                            Text(
                                text = if (uiState.tagSuggestions.isNotEmpty()) "Matching tags" else "Existing tags",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                visibleExistingTags.forEach { tag ->
                                    AssistChip(
                                        onClick = { onSelectTag(tag.name) },
                                        label = { Text(tag.name) },
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        if (uiState.tagDraft.isNotBlank() &&
                            uiState.selectedTags.none { it.equals(uiState.tagDraft.trim(), ignoreCase = true) }
                        ) {
                            TextButton(onClick = onCreateTag) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Create tag “${uiState.tagDraft.trim()}”")
                            }
                        }
                    }
                }

                item {
                    WebsiteMetadataCard(
                        uiState = uiState,
                        onNoteChanged = onNoteChanged,
                        onReasonSavedChanged = onReasonSavedChanged,
                        onSourceLabelChanged = onSourceLabelChanged,
                        onCustomLabelChanged = onCustomLabelChanged,
                        onPrioritySelected = onPrioritySelected,
                        onFollowUpStatusSelected = onFollowUpStatusSelected,
                        onRevisitDateChanged = onRevisitDateChanged,
                    )
                }

                uiState.metadataPreview?.let { preview ->
                    item {
                        MetadataPreviewCard(
                            preview = preview,
                            smartCapture = uiState.smartCapture,
                        )
                    }
                }

                item {
                    Button(
                        onClick = onSubmit,
                        enabled = !uiState.isSubmitting && uiState.rawUrl.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        when {
                            uiState.isSubmitting -> {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.size(8.dp))
                            }
                            uiState.phase == AddWebsitePhase.SUCCESS -> {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.size(8.dp))
                            }
                        }
                        Text(uiState.submitLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscardChangesDialog(
    onDismiss: () -> Unit,
    onDiscardChanges: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Discard changes?") },
        text = {
            Text(
                "You have unsaved changes in this form. If you leave now, LinkNest will discard them.",
            )
        },
        confirmButton = {
            TextButton(onClick = onDiscardChanges) {
                Text("Discard")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep editing")
            }
        },
    )
}

@Composable
private fun WebsiteMetadataCard(
    uiState: AddEditUiState,
    onNoteChanged: (String) -> Unit,
    onReasonSavedChanged: (String) -> Unit,
    onSourceLabelChanged: (String) -> Unit,
    onCustomLabelChanged: (String) -> Unit,
    onPrioritySelected: (WebsitePriority) -> Unit,
    onFollowUpStatusSelected: (FollowUpStatus) -> Unit,
    onRevisitDateChanged: (String) -> Unit,
) {
    GlassPanel {
        Text("Notes and follow-up", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.note,
            onValueChange = onNoteChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Note or annotation") },
            minLines = 3,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.reasonSaved,
            onValueChange = onReasonSavedChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Why this was saved") },
            minLines = 2,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.sourceLabel,
            onValueChange = onSourceLabelChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Source label") },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.customLabel,
            onValueChange = onCustomLabelChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Custom label") },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("Priority", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WebsitePriority.entries.forEach { priority ->
                FilterChip(
                    selected = uiState.priority == priority,
                    onClick = { onPrioritySelected(priority) },
                    label = { Text(priority.displayName()) },
                    leadingIcon = {
                        if (priority == WebsitePriority.CRITICAL) {
                            Icon(Icons.Rounded.PriorityHigh, contentDescription = null)
                        }
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Follow-up", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FollowUpStatus.entries.forEach { status ->
                FilterChip(
                    selected = uiState.followUpStatus == status,
                    onClick = { onFollowUpStatusSelected(status) },
                    label = { Text(status.displayName()) },
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.revisitDateInput,
            onValueChange = onRevisitDateChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Revisit date (YYYY-MM-DD)") },
            singleLine = true,
        )
    }
}

@Composable
private fun SmartCaptureCard(
    smartCapture: SmartCaptureResult,
    onSelectSuggestedTag: (String) -> Unit,
    alreadySelectedTags: List<String>,
) {
    GlassPanel {
        Text("Smart capture", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        smartCapture.cleanedTitle?.let { cleanedTitle ->
            Spacer(modifier = Modifier.height(12.dp))
            Text("Title cleanup suggestion", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(cleanedTitle, fontWeight = FontWeight.Medium)
        }
        if (smartCapture.suggestedTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Suggested tags", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                smartCapture.suggestedTags.forEach { tag ->
                    val isSelected = alreadySelectedTags.any { it.equals(tag, ignoreCase = true) }
                    FilterChip(
                        selected = isSelected,
                        onClick = { if (!isSelected) onSelectSuggestedTag(tag) },
                        label = { Text(tag) },
                    )
                }
            }
        }
        smartCapture.finalUrl?.let { finalUrl ->
            Spacer(modifier = Modifier.height(12.dp))
            Text("Resolved destination", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(finalUrl, style = MaterialTheme.typography.bodyMedium)
        }
        smartCapture.iconConfidenceLabel?.let { label ->
            Spacer(modifier = Modifier.height(12.dp))
            AssistChip(onClick = {}, label = { Text(label) })
        }
        val warnings = listOfNotNull(
            smartCapture.loginRequiredHint,
            smartCapture.suspiciousWarning,
            smartCapture.lowConfidenceWarning,
        )
        if (warnings.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            warnings.forEach { warning ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.58f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(
                        text = warning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DuplicateReviewCard(
    duplicateCheck: DuplicateCheckResult,
    onResolveDuplicate: (DuplicateDecision) -> Unit,
) {
    GlassPanel {
        Text("Possible duplicate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "LinkNest found a similar saved website. Decide how to continue before persisting this entry.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        duplicateCheck.matches.forEach { match ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(match.title, fontWeight = FontWeight.SemiBold)
                    Text(match.normalizedUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${match.categoryName} · ${match.type.displayName()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DuplicateDecision.entries.forEach { decision ->
                AssistChip(
                    onClick = { onResolveDuplicate(decision) },
                    label = { Text(decision.displayName()) },
                )
            }
        }
    }
}

@Composable
private fun CategoryPicker(
    categories: List<SelectableCategory>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long) -> Unit,
) {
    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (selectedCategory != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(selectedCategory.colorHex)).copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(selectedCategory.iconValue ?: selectedCategory.name.take(1))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Selected category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(selectedCategory.name, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        if (categories.isEmpty()) {
            Text(
                text = "Create a category to organize this website.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }
        Text("Existing categories", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { category ->
                FilterChip(
                    selected = category.id == selectedCategoryId,
                    onClick = { onCategorySelected(category.id) },
                    label = { Text(category.name) },
                    leadingIcon = { Text(category.iconValue ?: category.name.take(1)) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CreateCategorySheet(
    onDismiss: () -> Unit,
    onCreateCategory: (CategoryDraft) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf(CategoryColors.first()) }
    var iconValue by remember { mutableStateOf(CategoryIcons.first()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Create Category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Category name") },
                singleLine = true,
            )
            Text("Color", fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CategoryColors.forEach { candidate ->
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { colorHex = candidate },
                        color = Color(android.graphics.Color.parseColor(candidate)),
                        tonalElevation = if (candidate == colorHex) 4.dp else 0.dp,
                    ) {}
                }
            }
            Text("Icon", fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CategoryIcons.forEach { candidate ->
                    FilterChip(
                        selected = candidate == iconValue,
                        onClick = { iconValue = candidate },
                        label = { Text(candidate) },
                    )
                }
            }
            Button(
                onClick = {
                    onCreateCategory(
                        CategoryDraft(
                            name = name.trim(),
                            colorHex = colorHex,
                            iconType = IconType.EMOJI,
                            iconValue = iconValue,
                        ),
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Create category")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MetadataPreviewCard(
    preview: MetadataPreview,
    smartCapture: SmartCaptureResult?,
) {
    GlassPanel {
        Text("Inspection Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!preview.preferredIconUrl.isNullOrBlank()) {
                AsyncImage(
                    model = preview.preferredIconUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(preview.domain.take(1).uppercase())
                }
            }
            Column {
                Text(smartCapture?.cleanedTitle ?: preview.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(preview.domain, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                preview.suggestedCategory?.let { suggestion ->
                    Text("Suggested category: ${suggestion.categoryName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Normalized: ${preview.normalizedUrl}", style = MaterialTheme.typography.bodySmall)
        preview.finalUrl?.takeIf { it != preview.normalizedUrl }?.let { finalUrl ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Final: $finalUrl", style = MaterialTheme.typography.bodySmall)
        }
        smartCapture?.iconConfidenceLabel?.let { label ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun WebsitePriority.displayName(): String = when (this) {
    WebsitePriority.LOW -> "Low"
    WebsitePriority.NORMAL -> "Normal"
    WebsitePriority.HIGH -> "High"
    WebsitePriority.CRITICAL -> "Critical"
}

private fun FollowUpStatus.displayName(): String = when (this) {
    FollowUpStatus.NONE -> "None"
    FollowUpStatus.REVIEW -> "Needs review"
    FollowUpStatus.IN_PROGRESS -> "In progress"
    FollowUpStatus.WAITING -> "Waiting"
    FollowUpStatus.DONE -> "Done"
}

private fun DuplicateDecision.displayName(): String = when (this) {
    DuplicateDecision.KEEP_BOTH -> "Keep both"
    DuplicateDecision.CANCEL_SAVE -> "Cancel save"
    DuplicateDecision.REPLACE_EXISTING -> "Replace existing"
    DuplicateDecision.MERGE_METADATA -> "Merge metadata"
    DuplicateDecision.MOVE_EXISTING -> "Move existing"
}

private fun DuplicateMatchType.displayName(): String = when (this) {
    DuplicateMatchType.EXACT_URL -> "Exact URL"
    DuplicateMatchType.NORMALIZED_URL -> "Normalized URL"
    DuplicateMatchType.REDIRECTED_URL -> "Redirected target"
    DuplicateMatchType.EFFECTIVE_DESTINATION -> "Effective destination"
    DuplicateMatchType.TITLE_DOMAIN -> "Title and domain"
}
