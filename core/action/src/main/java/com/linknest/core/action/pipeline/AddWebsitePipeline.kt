package com.linknest.core.action.pipeline

import com.linknest.core.action.ActionIssue
import com.linknest.core.action.ActionResult
import com.linknest.core.action.action.DuplicateCheckAction
import com.linknest.core.action.action.FetchMetadataAction
import com.linknest.core.action.action.NormalizeUrlAction
import com.linknest.core.action.action.PersistWebsiteAction
import com.linknest.core.action.action.SmartCaptureAction
import com.linknest.core.action.action.SuggestCategoryAction
import com.linknest.core.action.action.UpdateDomainMappingAction
import com.linknest.core.action.action.ValidateUrlAction
import com.linknest.core.action.model.AddWebsitePipelineInput
import com.linknest.core.action.model.AddWebsitePipelineOutput
import com.linknest.core.action.model.DuplicateCheckInput
import com.linknest.core.action.model.IconResolutionInput
import com.linknest.core.action.model.IconResolutionOutput
import com.linknest.core.action.model.SmartCaptureInput
import com.linknest.core.action.model.SuggestCategoryInput
import com.linknest.core.action.model.UpdateDomainMappingInput
import com.linknest.core.data.mapper.asPreview
import com.linknest.core.data.model.PersistWebsiteRequest
import com.linknest.core.data.repository.WebsiteRepository
import com.linknest.core.data.usecase.EnsureTagsUseCase
import com.linknest.core.model.AddWebsitePhase
import com.linknest.core.model.DuplicateDecision
import com.linknest.core.model.HealthStatus
import com.linknest.core.model.IconSource
import com.linknest.core.network.model.MetadataResult
import javax.inject.Inject

class AddWebsitePipeline @Inject constructor(
    private val normalizeUrlAction: NormalizeUrlAction,
    private val validateUrlAction: ValidateUrlAction,
    private val fetchMetadataAction: FetchMetadataAction,
    private val suggestCategoryAction: SuggestCategoryAction,
    private val smartCaptureAction: SmartCaptureAction,
    private val duplicateCheckAction: DuplicateCheckAction,
    private val iconResolutionPipeline: IconResolutionPipeline,
    private val persistWebsiteAction: PersistWebsiteAction,
    private val updateDomainMappingAction: UpdateDomainMappingAction,
    private val ensureTagsUseCase: EnsureTagsUseCase,
    private val websiteRepository: WebsiteRepository,
) {
    suspend operator fun invoke(
        input: AddWebsitePipelineInput,
        onPhaseChanged: (AddWebsitePhase) -> Unit = {},
    ): ActionResult<AddWebsitePipelineOutput> {
        val issues = mutableListOf<ActionIssue>()
        val isEditMode = input.existingWebsiteId != null

        onPhaseChanged(AddWebsitePhase.SECURING_URL)
        val normalized = when (val result = normalizeUrlAction(input.rawUrl)) {
            is ActionResult.Success -> result.value
            is ActionResult.PartialSuccess -> result.value
            is ActionResult.Failure -> return result
        }

        when (val validation = validateUrlAction(normalized)) {
            is ActionResult.Failure -> return validation
            is ActionResult.PartialSuccess -> issues += validation.issues
            is ActionResult.Success -> Unit
        }

        onPhaseChanged(AddWebsitePhase.INSPECTING_WEBSITE)
        val metadata = input.preview
            ?.takeIf { preview -> preview.normalizedUrl == normalized.normalizedUrl }
            ?.toMetadataResult()
            ?: when (val result = fetchMetadataAction(normalized)) {
                is ActionResult.Success -> result.value
                is ActionResult.PartialSuccess -> {
                    issues += result.issues
                    result.value
                }
                is ActionResult.Failure -> {
                    issues += result.issue
                    MetadataResult(
                        title = normalized.domain,
                        canonicalUrl = null,
                        finalUrl = normalized.normalizedUrl,
                        domain = normalized.domain,
                        ogImageUrl = null,
                        faviconUrl = null,
                        chosenIconSource = IconSource.GENERATED,
                    )
                }
            }

        onPhaseChanged(AddWebsitePhase.SUGGESTING_CATEGORY)
        val suggestion = when (val result = suggestCategoryAction(
            SuggestCategoryInput(
                domain = normalized.domain,
                contextHint = metadata.title,
            ),
        )) {
            is ActionResult.Success -> result.value
            is ActionResult.PartialSuccess -> {
                issues += result.issues
                result.value
            }
            is ActionResult.Failure -> {
                issues += result.issue
                null
            }
        }

        val preview = metadata.asPreview(normalized.normalizedUrl).copy(suggestedCategory = suggestion)

        onPhaseChanged(AddWebsitePhase.RUNNING_SMART_CAPTURE)
        val smartCapture = when (val result = smartCaptureAction(
            SmartCaptureInput(
                normalizedUrl = normalized,
                preview = preview,
                suggestedCategory = suggestion,
            ),
        )) {
            is ActionResult.Success -> result.value
            is ActionResult.PartialSuccess -> {
                issues += result.issues
                result.value
            }
            is ActionResult.Failure -> {
                issues += result.issue
                com.linknest.core.model.SmartCaptureResult(suggestedCategory = suggestion)
            }
        }

        val categoryId = input.selectedCategoryId ?: smartCapture.suggestedCategory?.categoryId ?: suggestion?.categoryId
        if (categoryId == null) {
            return ActionResult.Failure(
                issue = ActionIssue(
                    code = "CATEGORY_REQUIRED",
                    message = "Select or accept a category before saving.",
                ),
            )
        }

        onPhaseChanged(AddWebsitePhase.REVIEWING_DUPLICATES)
        val duplicateCheck = when (val result = duplicateCheckAction(
            DuplicateCheckInput(
                normalizedUrl = normalized.normalizedUrl,
                finalUrl = metadata.finalUrl,
                domain = normalized.domain,
                title = smartCapture.cleanedTitle ?: metadata.title,
                excludeWebsiteId = input.existingWebsiteId,
            ),
        )) {
            is ActionResult.Success -> result.value
            is ActionResult.PartialSuccess -> {
                issues += result.issues
                result.value
            }
            is ActionResult.Failure -> {
                issues += result.issue
                null
            }
        }

        if (!isEditMode && duplicateCheck?.hasDuplicates == true && input.duplicateDecision == null) {
            val output = AddWebsitePipelineOutput(
                websiteId = null,
                categoryId = categoryId,
                normalizedUrl = normalized.normalizedUrl,
                metadataPreview = preview,
                categorySuggestion = suggestion,
                smartCapture = smartCapture,
                duplicateCheck = duplicateCheck,
                cachedIconUri = null,
                requiresDuplicateDecision = true,
                wasPersisted = false,
            )
            val duplicateIssue = ActionIssue(
                code = "DUPLICATE_DECISION_REQUIRED",
                message = "A similar website is already saved. Choose how to proceed.",
            )
            return ActionResult.PartialSuccess(output, issues + duplicateIssue)
        }

        if (input.duplicateDecision == DuplicateDecision.CANCEL_SAVE) {
            return ActionResult.Failure(
                issue = ActionIssue(
                    code = "SAVE_CANCELLED",
                    message = "Save cancelled.",
                ),
            )
        }

        val resolvedExistingWebsiteId = if (isEditMode) {
            input.existingWebsiteId
        } else {
            when (input.duplicateDecision) {
                DuplicateDecision.REPLACE_EXISTING,
                DuplicateDecision.MERGE_METADATA,
                DuplicateDecision.MOVE_EXISTING,
                -> duplicateCheck?.primaryMatch?.websiteId
                else -> null
            }
        }

        onPhaseChanged(AddWebsitePhase.RESOLVING_ICON)
        val iconResolution = when (val result = iconResolutionPipeline(
            IconResolutionInput(
                customIconUri = input.customIconUri,
                emojiIcon = input.emojiIcon,
                ogImageUrl = metadata.ogImageUrl,
                faviconUrl = metadata.faviconUrl,
                preferredSource = metadata.chosenIconSource,
                fetchedAt = System.currentTimeMillis(),
            ),
        )) {
            is ActionResult.Success -> result.value
            is ActionResult.PartialSuccess -> {
                issues += result.issues
                result.value
            }
            is ActionResult.Failure -> {
                issues += result.issue
                IconResolutionOutput(
                    chosenIconSource = IconSource.GENERATED,
                    sourceUrl = null,
                    persistedIconCache = null,
                )
            }
        }

        val resolvedTags = ensureTagsUseCase((input.tagNames + smartCapture.suggestedTags).distinct())
        val now = System.currentTimeMillis()
        val existingWebsite = resolvedExistingWebsiteId?.let { id ->
            websiteRepository.getWebsiteById(id)
        }
        val nextSortOrder = existingWebsite
            ?.let { existing ->
                if (existing.categoryId == categoryId) existing.sortOrder else null
            }
            ?: websiteRepository.getNextSortOrder(categoryId)
        val healthStatus = when {
            !smartCapture.loginRequiredHint.isNullOrBlank() -> HealthStatus.LOGIN_REQUIRED
            metadata.finalUrl != normalized.normalizedUrl -> HealthStatus.REDIRECTED
            metadata.ogImageUrl == null && metadata.faviconUrl == null -> HealthStatus.UNKNOWN
            else -> HealthStatus.OK
        }

        onPhaseChanged(AddWebsitePhase.SAVING)
        val persistResult = persistWebsiteAction(
            PersistWebsiteRequest(
                existingWebsiteId = resolvedExistingWebsiteId,
                categoryId = categoryId,
                title = (smartCapture.cleanedTitle ?: metadata.title).ifBlank { normalized.domain },
                canonicalUrl = metadata.canonicalUrl,
                finalUrl = metadata.finalUrl,
                normalizedUrl = normalized.normalizedUrl,
                domain = normalized.domain,
                ogImageUrl = metadata.ogImageUrl,
                faviconUrl = metadata.faviconUrl,
                chosenIconSource = iconResolution.chosenIconSource,
                customIconUri = input.customIconUri,
                emojiIcon = input.emojiIcon,
                tileSizeDp = input.tileSizeDp,
                sortOrder = nextSortOrder,
                isPinned = input.isPinned,
                healthStatus = healthStatus,
                note = input.note,
                reasonSaved = input.reasonSaved,
                priority = input.priority,
                followUpStatus = input.followUpStatus,
                revisitAt = input.revisitAt,
                sourceLabel = input.sourceLabel,
                customLabel = input.customLabel,
                createdAt = now,
                updatedAt = now,
                lastCheckedAt = now,
                tagIds = resolvedTags.map { it.id },
                iconCache = iconResolution.persistedIconCache,
            ),
        )
        val websiteId = when (persistResult) {
            is ActionResult.Success -> persistResult.value
            is ActionResult.PartialSuccess -> {
                issues += persistResult.issues
                persistResult.value
            }
            is ActionResult.Failure -> return persistResult
        }

        when (val mappingResult = updateDomainMappingAction(
            UpdateDomainMappingInput(
                domain = normalized.domain,
                categoryId = categoryId,
            ),
        )) {
            is ActionResult.Failure -> issues += mappingResult.issue
            is ActionResult.PartialSuccess -> issues += mappingResult.issues
            is ActionResult.Success -> Unit
        }

        onPhaseChanged(AddWebsitePhase.SUCCESS)
        val output = AddWebsitePipelineOutput(
            websiteId = websiteId,
            categoryId = categoryId,
            normalizedUrl = normalized.normalizedUrl,
            metadataPreview = preview,
            categorySuggestion = suggestion,
            smartCapture = smartCapture,
            duplicateCheck = duplicateCheck,
            cachedIconUri = iconResolution.persistedIconCache?.localUri,
            requiresDuplicateDecision = false,
            wasPersisted = true,
        )
        return if (issues.isEmpty()) {
            ActionResult.Success(output)
        } else {
            ActionResult.PartialSuccess(output, issues)
        }
    }

    private fun com.linknest.core.model.MetadataPreview.toMetadataResult(): MetadataResult = MetadataResult(
        title = title,
        canonicalUrl = canonicalUrl,
        finalUrl = finalUrl ?: normalizedUrl,
        domain = domain,
        ogImageUrl = ogImageUrl,
        faviconUrl = faviconUrl,
        chosenIconSource = chosenIconSource,
    )
}
