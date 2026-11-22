package com.linknest.feature.addedit

import androidx.core.graphics.toColorInt
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.linknest.core.designsystem.categoryColorOptions
import com.linknest.core.designsystem.categoryIconOptions
import com.linknest.core.designsystem.component.GlassPanel
import com.linknest.core.designsystem.component.LinkNestGradientBackground
import com.linknest.core.designsystem.component.rememberFullyExpandedSheetState
import com.linknest.core.designsystem.labelResource
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

@Composable
fun AddEditRoute(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDiscardChangesDialog by rememberSaveable { mutableStateOf(false) }

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
    var categorySheetOpen by rememberSaveable { mutableStateOf(false) }

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
            modifier = Modifier.imePadding(),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(if (uiState.isEditMode) stringResource(R.string.addedit_edit_title) else stringResource(R.string.addedit_add_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.addedit_back))
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
                        Text(stringResource(R.string.addedit_url_input), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.addedit_url_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = uiState.rawUrl,
                            onValueChange = onUrlChanged,
                            label = { Text(stringResource(R.string.addedit_url)) },
                            leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        uiState.phaseLabelResource?.let { phaseResource ->
                            val phaseLabel = stringResource(phaseResource)
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().semantics { contentDescription = phaseLabel })
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(phaseLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                item {
                    GlassPanel {
                        Text(stringResource(R.string.addedit_category), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                                Text(stringResource(R.string.addedit_create_category))
                            }
                            uiState.suggestedCategory?.let { suggestion ->
                                AssistChip(
                                    onClick = onUseSuggestedCategory,
                                    label = { Text(stringResource(R.string.addedit_suggested, suggestion.categoryName)) },
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
                        Text(stringResource(R.string.addedit_tags), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(12.dp))
                        val tagInputLabel = stringResource(R.string.addedit_search_tags)
                        OutlinedTextField(
                            value = uiState.tagDraft,
                            onValueChange = onTagDraftChanged,
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = tagInputLabel },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            label = { Text(tagInputLabel) },
                            singleLine = true,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (uiState.selectedTags.isNotEmpty()) {
                            Text(stringResource(R.string.addedit_selected_tags), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                uiState.selectedTags.forEach { tag ->
                                    val removeTagLabel = stringResource(R.string.addedit_remove_tag, tag)
                                    FilterChip(
                                        selected = true,
                                        onClick = { onRemoveTag(tag) },
                                        modifier = Modifier.semantics { onClick(label = removeTagLabel, action = null) },
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
                                text = if (uiState.tagSuggestions.isNotEmpty()) stringResource(R.string.addedit_matching_tags) else stringResource(R.string.addedit_existing_tags),
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
                                Text(stringResource(R.string.addedit_create_tag, uiState.tagDraft.trim()))
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
                        Text(stringResource(if (uiState.isEditMode) R.string.addedit_save_changes else R.string.addedit_save))
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
        title = { Text(stringResource(R.string.addedit_discard_title)) },
        text = {
            Text(
                stringResource(R.string.addedit_discard_body),
            )
        },
        confirmButton = {
            TextButton(onClick = onDiscardChanges) {
                Text(stringResource(R.string.addedit_discard))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.addedit_keep_editing))
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
        Text(stringResource(R.string.addedit_notes_follow_up), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.note,
            onValueChange = onNoteChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.addedit_note)) },
            minLines = 3,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.reasonSaved,
            onValueChange = onReasonSavedChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.addedit_why_saved)) },
            minLines = 2,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.sourceLabel,
            onValueChange = onSourceLabelChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.addedit_source_label)) },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.customLabel,
            onValueChange = onCustomLabelChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.addedit_custom_label)) },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(stringResource(R.string.addedit_priority), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WebsitePriority.entries.forEach { priority ->
                FilterChip(
                    selected = uiState.priority == priority,
                    onClick = { onPrioritySelected(priority) },
                    label = { Text(stringResource(priority.labelResource)) },
                    leadingIcon = {
                        if (priority == WebsitePriority.CRITICAL) {
                            Icon(Icons.Rounded.PriorityHigh, contentDescription = null)
                        }
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(stringResource(R.string.addedit_follow_up), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FollowUpStatus.entries.forEach { status ->
                FilterChip(
                    selected = uiState.followUpStatus == status,
                    onClick = { onFollowUpStatusSelected(status) },
                    label = { Text(stringResource(status.labelResource)) },
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.revisitDateInput,
            onValueChange = onRevisitDateChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.addedit_revisit_date)) },
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
        Text(stringResource(R.string.addedit_smart_capture), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        smartCapture.cleanedTitle?.let { cleanedTitle ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.addedit_clean_title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(cleanedTitle, fontWeight = FontWeight.Medium)
        }
        if (smartCapture.suggestedTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.addedit_suggested_tags), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Text(stringResource(R.string.addedit_resolved_destination), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Text(stringResource(R.string.addedit_possible_duplicate), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.addedit_duplicate_help),
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
                    Text(stringResource(R.string.addedit_duplicate_description, match.categoryName, stringResource(match.type.labelResource)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DuplicateDecision.entries.forEach { decision ->
                AssistChip(
                    onClick = { onResolveDuplicate(decision) },
                    label = { Text(stringResource(decision.labelResource)) },
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
                            .background(Color(selectedCategory.colorHex.toColorInt()).copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(selectedCategory.iconValue ?: selectedCategory.name.take(1))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.addedit_selected_category), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(selectedCategory.name, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        if (categories.isEmpty()) {
            Text(
                text = stringResource(R.string.addedit_new_category_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }
        Text(stringResource(R.string.addedit_existing_categories), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    var name by rememberSaveable { mutableStateOf("") }
    var colorHex by rememberSaveable { mutableStateOf(categoryColorOptions.first().first) }
    var iconValue by rememberSaveable { mutableStateOf(categoryIconOptions.first().first) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberFullyExpandedSheetState(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.addedit_create_category_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.addedit_category_name)) },
                singleLine = true,
            )
            Text(stringResource(R.string.addedit_color), fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                categoryColorOptions.forEach { (candidate, labelResource) ->
                    val label = stringResource(labelResource)
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = label; selected = candidate == colorHex }
                            .clip(CircleShape)
                            .clickable { colorHex = candidate },
                        color = Color(candidate.toColorInt()),
                        tonalElevation = if (candidate == colorHex) 4.dp else 0.dp,
                    ) {}
                }
            }
            Text(stringResource(R.string.addedit_icon), fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                categoryIconOptions.forEach { (candidate, labelResource) ->
                    val label = stringResource(labelResource)
                    FilterChip(
                        selected = candidate == iconValue,
                        onClick = { iconValue = candidate },
                        modifier = Modifier.semantics { contentDescription = label },
                        label = { Text(candidate, modifier = Modifier.clearAndSetSemantics {}) },
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
                Text(stringResource(R.string.addedit_create_category))
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
        Text(stringResource(R.string.addedit_preview), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                    Text(stringResource(R.string.addedit_suggested_category, suggestion.categoryName), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = stringResource(R.string.addedit_normalized, preview.normalizedUrl), style = MaterialTheme.typography.bodySmall)
        preview.finalUrl?.takeIf { it != preview.normalizedUrl }?.let { finalUrl ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = stringResource(R.string.addedit_final_url, finalUrl), style = MaterialTheme.typography.bodySmall)
        }
        smartCapture?.iconConfidenceLabel?.let { label ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}


@get:androidx.annotation.StringRes
private val DuplicateDecision.labelResource: Int
    get() = when (this) {
        DuplicateDecision.KEEP_BOTH -> R.string.addedit_duplicate_keep_both
        DuplicateDecision.CANCEL_SAVE -> R.string.addedit_duplicate_cancel_save
        DuplicateDecision.REPLACE_EXISTING -> R.string.addedit_duplicate_replace_existing
        DuplicateDecision.MERGE_METADATA -> R.string.addedit_duplicate_merge_metadata
        DuplicateDecision.MOVE_EXISTING -> R.string.addedit_duplicate_move_existing
    }

@get:androidx.annotation.StringRes
private val DuplicateMatchType.labelResource: Int
    get() = when (this) {
        DuplicateMatchType.EXACT_URL -> R.string.addedit_duplicate_exact_url
        DuplicateMatchType.NORMALIZED_URL -> R.string.addedit_duplicate_normalized_url
        DuplicateMatchType.REDIRECTED_URL -> R.string.addedit_duplicate_redirected_url
        DuplicateMatchType.EFFECTIVE_DESTINATION -> R.string.addedit_duplicate_effective_destination
        DuplicateMatchType.TITLE_DOMAIN -> R.string.addedit_duplicate_title_domain
    }
