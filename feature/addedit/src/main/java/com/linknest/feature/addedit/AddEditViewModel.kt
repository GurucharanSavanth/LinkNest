package com.linknest.feature.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linknest.core.action.ActionResult
import com.linknest.core.action.action.NormalizeUrlAction
import com.linknest.core.action.action.SuggestCategoryAction
import com.linknest.core.action.model.AddWebsitePipelineInput
import com.linknest.core.action.model.SuggestCategoryInput
import com.linknest.core.action.pipeline.AddWebsitePipeline
import com.linknest.core.data.usecase.AddTagUseCase
import com.linknest.core.data.usecase.CreateCategoryUseCase
import com.linknest.core.data.usecase.FilterByTagUseCase
import com.linknest.core.data.usecase.GetWebsiteUseCase
import com.linknest.core.data.usecase.ObserveSelectableCategoriesUseCase
import com.linknest.core.data.usecase.ObserveTagsUseCase
import com.linknest.core.model.AddWebsitePhase
import com.linknest.core.model.CategoryDraft
import com.linknest.core.model.CategorySuggestion
import com.linknest.core.model.DuplicateCheckResult
import com.linknest.core.model.DuplicateDecision
import com.linknest.core.model.FollowUpStatus
import com.linknest.core.model.MetadataPreview
import com.linknest.core.model.SelectableCategory
import com.linknest.core.model.SmartCaptureResult
import com.linknest.core.model.TagModel
import com.linknest.core.model.WebsitePriority
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditUiState(
    val existingWebsiteId: Long? = null,
    val rawUrl: String = "",
    val categories: List<SelectableCategory> = emptyList(),
    val selectedCategoryId: Long? = null,
    val suggestedCategory: CategorySuggestion? = null,
    val metadataPreview: MetadataPreview? = null,
    val smartCapture: SmartCaptureResult? = null,
    val duplicateCheck: DuplicateCheckResult? = null,
    val awaitingDuplicateDecision: Boolean = false,
    val availableTags: List<TagModel> = emptyList(),
    val selectedTags: List<String> = emptyList(),
    val tagDraft: String = "",
    val tagSuggestions: List<TagModel> = emptyList(),
    val note: String = "",
    val reasonSaved: String = "",
    val sourceLabel: String = "",
    val customLabel: String = "",
    val revisitDateInput: String = "",
    val priority: WebsitePriority = WebsitePriority.NORMAL,
    val followUpStatus: FollowUpStatus = FollowUpStatus.NONE,
    // Preserved from existing website — not editable in this screen, kept to prevent data loss on save
    val isPinned: Boolean = false,
    val emojiIcon: String? = null,
    val customIconUri: String? = null,
    val tileSizeDp: Int? = null,
    val phase: AddWebsitePhase = AddWebsitePhase.IDLE,
    val isSubmitting: Boolean = false,
    val saveCompleted: Boolean = false,
    val userMessage: String? = null,
    val hasUnsavedChanges: Boolean = false,
) {
    val isEditMode: Boolean
        get() = existingWebsiteId != null

    val submitLabel: String
        get() = if (isEditMode) "Inspect & Save Changes" else "Inspect & Save"

    val phaseLabel: String?
        get() = when (phase) {
            AddWebsitePhase.IDLE -> null
            AddWebsitePhase.SECURING_URL -> "Securing URL"
            AddWebsitePhase.INSPECTING_WEBSITE -> "Inspecting website"
            AddWebsitePhase.SUGGESTING_CATEGORY -> "Resolving category"
            AddWebsitePhase.RUNNING_SMART_CAPTURE -> "Building smart suggestions"
            AddWebsitePhase.REVIEWING_DUPLICATES -> "Reviewing duplicates"
            AddWebsitePhase.RESOLVING_ICON -> "Resolving icon"
            AddWebsitePhase.SAVING -> "Saving"
            AddWebsitePhase.SUCCESS -> "Saved"
        }
}

private data class AddEditFormSnapshot(
    val rawUrl: String,
    val selectedCategoryId: Long?,
    val selectedTags: List<String>,
    val note: String,
    val reasonSaved: String,
    val sourceLabel: String,
    val customLabel: String,
    val revisitDateInput: String,
    val priority: WebsitePriority,
    val followUpStatus: FollowUpStatus,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class AddEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeSelectableCategoriesUseCase: ObserveSelectableCategoriesUseCase,
    observeTagsUseCase: ObserveTagsUseCase,
    private val getWebsiteUseCase: GetWebsiteUseCase,
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val addTagUseCase: AddTagUseCase,
    private val filterByTagUseCase: FilterByTagUseCase,
    private val normalizeUrlAction: NormalizeUrlAction,
    private val suggestCategoryAction: SuggestCategoryAction,
    private val addWebsitePipeline: AddWebsitePipeline,
) : ViewModel() {
    private val existingWebsiteId = savedStateHandle.get<Long>(ADD_EDIT_WEBSITE_ID_ARG)?.takeIf { it > 0L }
    private val urlFlow = MutableStateFlow(savedStateHandle.get<String>(ADD_EDIT_URL_ARG).orEmpty())
    private val tagDraftFlow = MutableStateFlow("")
    private val _uiState = MutableStateFlow(
        AddEditUiState(
            existingWebsiteId = existingWebsiteId,
            rawUrl = savedStateHandle.get<String>(ADD_EDIT_URL_ARG).orEmpty(),
        ),
    )
    private var baselineSnapshot = _uiState.value.toFormSnapshot()
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSelectableCategoriesUseCase().collect { categories ->
                var autoAssignedCategoryId: Long? = null
                _uiState.update { state ->
                    val selectedCategoryId = state.selectedCategoryId ?: categories.firstOrNull()?.id?.also {
                        autoAssignedCategoryId = it
                    }
                    state.copy(
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                    ).withUnsavedChanges(computeHasUnsavedChangesFor)
                }
                if (!uiState.value.isEditMode &&
                    autoAssignedCategoryId != null &&
                    baselineSnapshot.selectedCategoryId == null &&
                    !uiState.value.hasUnsavedChanges
                ) {
                    baselineSnapshot = baselineSnapshot.copy(selectedCategoryId = autoAssignedCategoryId)
                }
            }
        }

        viewModelScope.launch {
            observeTagsUseCase().collect { tags ->
                _uiState.update { it.copy(availableTags = tags) }
            }
        }

        urlFlow
            .debounce(300)
            .distinctUntilChanged()
            .onEach(::updateSuggestion)
            .launchIn(viewModelScope)

        tagDraftFlow
            .debounce(180)
            .distinctUntilChanged()
            .onEach(::updateTagSuggestions)
            .launchIn(viewModelScope)

        if (existingWebsiteId != null) {
            viewModelScope.launch {
                getWebsiteUseCase(existingWebsiteId)?.let { website ->
                    val loadedState = _uiState.value.copy(
                        rawUrl = website.normalizedUrl,
                        selectedCategoryId = website.categoryId,
                        selectedTags = website.tagNames,
                        note = website.note.orEmpty(),
                        reasonSaved = website.reasonSaved.orEmpty(),
                        sourceLabel = website.sourceLabel.orEmpty(),
                        customLabel = website.customLabel.orEmpty(),
                        revisitDateInput = website.revisitAt?.let(::formatEpochDay).orEmpty(),
                        priority = website.priority,
                        followUpStatus = website.followUpStatus,
                        isPinned = website.isPinned,
                        emojiIcon = website.emojiIcon,
                        customIconUri = website.customIconUri,
                        tileSizeDp = website.tileSizeDp,
                        metadataPreview = MetadataPreview(
                            title = website.title,
                            normalizedUrl = website.normalizedUrl,
                            canonicalUrl = website.canonicalUrl,
                            finalUrl = website.finalUrl,
                            domain = website.domain,
                            ogImageUrl = website.ogImageUrl,
                            faviconUrl = website.faviconUrl,
                            chosenIconSource = website.chosenIconSource,
                        ),
                    )
                    baselineSnapshot = loadedState.toFormSnapshot()
                    _uiState.value = loadedState.copy(hasUnsavedChanges = false)
                    urlFlow.value = website.normalizedUrl
                }
            }
        }
    }

    fun onUrlChanged(rawUrl: String) {
        updateFormState {
            it.copy(
                rawUrl = rawUrl,
                metadataPreview = null,
                smartCapture = null,
                duplicateCheck = null,
                awaitingDuplicateDecision = false,
                phase = AddWebsitePhase.IDLE,
            )
        }
        urlFlow.value = rawUrl
    }

    fun onCategorySelected(categoryId: Long) {
        updateFormState { it.copy(selectedCategoryId = categoryId) }
    }

    fun onUseSuggestedCategory() {
        _uiState.value.suggestedCategory?.let { suggestion ->
            updateFormState { it.copy(selectedCategoryId = suggestion.categoryId) }
        }
    }

    fun onCreateCategory(draft: CategoryDraft) {
        viewModelScope.launch {
            runCatching { createCategoryUseCase(draft) }
                .onSuccess { created ->
                    updateFormState {
                        it.copy(
                            selectedCategoryId = created.id,
                            userMessage = "Category ${created.name} created.",
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(userMessage = throwable.message ?: "Unable to create category.")
                    }
                }
        }
    }

    fun onTagDraftChanged(tagDraft: String) {
        _uiState.update { it.copy(tagDraft = tagDraft) }
        tagDraftFlow.value = tagDraft
    }

    fun onSelectTag(tagName: String) {
        val normalizedTag = tagName.trim()
        if (normalizedTag.isBlank()) return
        updateFormState { state ->
            state.copy(
                selectedTags = (state.selectedTags + normalizedTag).distinctBy(String::lowercase),
                tagDraft = "",
                tagSuggestions = emptyList(),
            )
        }
        tagDraftFlow.value = ""
    }

    fun onCreateTag() {
        val tagDraft = uiState.value.tagDraft.trim()
        if (tagDraft.isBlank()) return
        viewModelScope.launch {
            val resolvedTag = addTagUseCase(tagDraft)
            if (resolvedTag == null) {
                _uiState.update { it.copy(userMessage = "Unable to create tag.") }
                return@launch
            }
            onSelectTag(resolvedTag.name)
        }
    }

    fun onRemoveTag(tagName: String) {
        updateFormState { state ->
            state.copy(
                selectedTags = state.selectedTags.filterNot { it.equals(tagName, ignoreCase = true) },
            )
        }
    }

    fun onNoteChanged(note: String) {
        updateFormState { it.copy(note = note) }
    }

    fun onReasonSavedChanged(reason: String) {
        updateFormState { it.copy(reasonSaved = reason) }
    }

    fun onSourceLabelChanged(sourceLabel: String) {
        updateFormState { it.copy(sourceLabel = sourceLabel) }
    }

    fun onCustomLabelChanged(customLabel: String) {
        updateFormState { it.copy(customLabel = customLabel) }
    }

    fun onPrioritySelected(priority: WebsitePriority) {
        updateFormState { it.copy(priority = priority) }
    }

    fun onFollowUpStatusSelected(status: FollowUpStatus) {
        updateFormState { it.copy(followUpStatus = status) }
    }

    fun onRevisitDateChanged(value: String) {
        updateFormState { it.copy(revisitDateInput = value) }
    }

    fun onSubmit() {
        submitWithDecision(null)
    }

    fun onResolveDuplicate(decision: DuplicateDecision) {
        submitWithDecision(decision)
    }

    fun onSaveCompletedConsumed() {
        _uiState.update { it.copy(saveCompleted = false) }
    }

    fun onMessageConsumed() {
        _uiState.update { it.copy(userMessage = null) }
    }

    private fun submitWithDecision(decision: DuplicateDecision?) {
        val state = uiState.value
        if (state.isSubmitting) return
        if (state.rawUrl.isBlank()) {
            _uiState.update { it.copy(userMessage = "Enter a website URL first.") }
            return
        }

        val revisitAt = parseRevisitDate(state.revisitDateInput)
        if (state.revisitDateInput.isNotBlank() && revisitAt == null) {
            _uiState.update {
                it.copy(userMessage = "Use YYYY-MM-DD for revisit date.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmitting = true,
                    phase = AddWebsitePhase.SECURING_URL,
                    userMessage = null,
                    awaitingDuplicateDecision = false,
                )
            }
            when (
                val result = addWebsitePipeline(
                    AddWebsitePipelineInput(
                        existingWebsiteId = state.existingWebsiteId,
                        rawUrl = state.rawUrl,
                        selectedCategoryId = state.selectedCategoryId,
                        preview = state.metadataPreview,
                        tagNames = state.selectedTags,
                        note = state.note.takeIf(String::isNotBlank),
                        reasonSaved = state.reasonSaved.takeIf(String::isNotBlank),
                        priority = state.priority,
                        followUpStatus = state.followUpStatus,
                        revisitAt = revisitAt,
                        sourceLabel = state.sourceLabel.takeIf(String::isNotBlank),
                        customLabel = state.customLabel.takeIf(String::isNotBlank),
                        isPinned = state.isPinned,
                        emojiIcon = state.emojiIcon,
                        customIconUri = state.customIconUri,
                        tileSizeDp = state.tileSizeDp,
                        duplicateDecision = decision,
                    ),
                    onPhaseChanged = { phase ->
                        _uiState.update { current -> current.copy(phase = phase) }
                    },
                )
            ) {
                is ActionResult.Success -> handlePipelineResult(result.value, null)
                is ActionResult.PartialSuccess -> handlePipelineResult(
                    result.value,
                    result.issues.joinToString("\n") { issue -> issue.message },
                )
                is ActionResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            phase = AddWebsitePhase.IDLE,
                            userMessage = result.issue.message,
                        )
                    }
                }
            }
        }
    }

    private suspend fun updateSuggestion(rawUrl: String) {
        if (rawUrl.isBlank()) {
            _uiState.update { it.copy(suggestedCategory = null, smartCapture = null) }
            return
        }
        val normalized = when (val result = normalizeUrlAction(rawUrl)) {
            is ActionResult.Success -> result.value
            is ActionResult.PartialSuccess -> result.value
            is ActionResult.Failure -> {
                _uiState.update { it.copy(suggestedCategory = null, smartCapture = null) }
                return
            }
        }
        when (val result = suggestCategoryAction(SuggestCategoryInput(domain = normalized.domain))) {
            is ActionResult.Success -> applySuggestion(result.value)
            is ActionResult.PartialSuccess -> applySuggestion(result.value)
            is ActionResult.Failure -> _uiState.update { it.copy(suggestedCategory = null, smartCapture = null) }
        }
    }

    private suspend fun updateTagSuggestions(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            _uiState.update { it.copy(tagSuggestions = emptyList()) }
            return
        }
        val suggestions = filterByTagUseCase(normalizedQuery)
            .filterNot { suggestion ->
                uiState.value.selectedTags.any { it.equals(suggestion.name, ignoreCase = true) }
            }
        _uiState.update { it.copy(tagSuggestions = suggestions) }
    }

    private fun applySuggestion(suggestion: CategorySuggestion?) {
        _uiState.update { state ->
            state.copy(
                suggestedCategory = suggestion,
                metadataPreview = state.metadataPreview?.copy(suggestedCategory = suggestion),
            )
        }
    }

    private suspend fun handlePipelineResult(
        output: com.linknest.core.action.model.AddWebsitePipelineOutput,
        userMessage: String?,
    ) {
        _uiState.update {
            it.copy(
                isSubmitting = false,
                phase = if (output.requiresDuplicateDecision) AddWebsitePhase.REVIEWING_DUPLICATES else AddWebsitePhase.SUCCESS,
                metadataPreview = output.metadataPreview,
                suggestedCategory = output.categorySuggestion,
                smartCapture = output.smartCapture,
                duplicateCheck = output.duplicateCheck,
                awaitingDuplicateDecision = output.requiresDuplicateDecision,
                saveCompleted = false,
                userMessage = userMessage,
                hasUnsavedChanges = if (output.requiresDuplicateDecision) it.hasUnsavedChanges else false,
            )
        }
        if (!output.requiresDuplicateDecision && output.wasPersisted) {
            baselineSnapshot = _uiState.value.toFormSnapshot()
        }
        if (!output.requiresDuplicateDecision && output.wasPersisted) {
            delay(900)
            _uiState.update { it.copy(saveCompleted = true) }
        }
    }

    private fun updateFormState(transform: (AddEditUiState) -> AddEditUiState) {
        _uiState.update { current ->
            transform(current).withUnsavedChanges(computeHasUnsavedChangesFor)
        }
    }

    private val computeHasUnsavedChangesFor: (AddEditUiState) -> Boolean = { state ->
        state.toFormSnapshot() != baselineSnapshot
    }

    private fun parseRevisitDate(raw: String): Long? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        return runCatching {
            LocalDate.parse(trimmed)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }

    private fun formatEpochDay(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()
}

private fun AddEditUiState.toFormSnapshot(): AddEditFormSnapshot = AddEditFormSnapshot(
    rawUrl = rawUrl.trim(),
    selectedCategoryId = selectedCategoryId,
    selectedTags = selectedTags.sortedBy(String::lowercase),
    note = note.trim(),
    reasonSaved = reasonSaved.trim(),
    sourceLabel = sourceLabel.trim(),
    customLabel = customLabel.trim(),
    revisitDateInput = revisitDateInput.trim(),
    priority = priority,
    followUpStatus = followUpStatus,
)

private fun AddEditUiState.withUnsavedChanges(
    hasUnsavedChanges: (AddEditUiState) -> Boolean,
): AddEditUiState = copy(hasUnsavedChanges = hasUnsavedChanges(this))
