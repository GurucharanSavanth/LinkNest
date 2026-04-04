package com.linknest.core.action.action

import com.linknest.core.action.ActionResult
import com.linknest.core.action.AppAction
import com.linknest.core.action.actionResult
import com.linknest.core.action.model.DuplicateCheckInput
import com.linknest.core.action.model.IntegrityOverviewInput
import com.linknest.core.action.model.IntegrityOverviewOutput
import com.linknest.core.action.model.SmartCaptureInput
import com.linknest.core.data.usecase.CheckDuplicateUseCase
import com.linknest.core.data.usecase.GetIntegrityOverviewUseCase
import com.linknest.core.data.usecase.RunSmartCaptureUseCase
import com.linknest.core.model.DuplicateCheckResult
import com.linknest.core.model.SmartCaptureResult
import javax.inject.Inject

class SmartCaptureAction @Inject constructor(
    private val runSmartCaptureUseCase: RunSmartCaptureUseCase,
) : AppAction<SmartCaptureInput, SmartCaptureResult> {
    override suspend fun invoke(input: SmartCaptureInput): ActionResult<SmartCaptureResult> = actionResult(
        code = "SMART_CAPTURE_FAILED",
        defaultMessage = "Unable to build smart capture suggestions.",
    ) {
        runSmartCaptureUseCase(
            normalizedUrl = input.normalizedUrl,
            metadataPreview = input.preview,
            suggestedCategory = input.suggestedCategory,
        )
    }
}

class DuplicateCheckAction @Inject constructor(
    private val checkDuplicateUseCase: CheckDuplicateUseCase,
) : AppAction<DuplicateCheckInput, DuplicateCheckResult> {
    override suspend fun invoke(input: DuplicateCheckInput): ActionResult<DuplicateCheckResult> = actionResult(
        code = "DUPLICATE_CHECK_FAILED",
        defaultMessage = "Unable to check for duplicate websites.",
    ) {
        checkDuplicateUseCase(
            normalizedUrl = input.normalizedUrl,
            finalUrl = input.finalUrl,
            domain = input.domain,
            title = input.title,
            excludeWebsiteId = input.excludeWebsiteId,
        )
    }
}

class GenerateIntegrityOverviewAction @Inject constructor(
    private val getIntegrityOverviewUseCase: GetIntegrityOverviewUseCase,
) : AppAction<IntegrityOverviewInput, IntegrityOverviewOutput> {
    override suspend fun invoke(input: IntegrityOverviewInput): ActionResult<IntegrityOverviewOutput> = actionResult(
        code = "INTEGRITY_OVERVIEW_FAILED",
        defaultMessage = "Unable to load the integrity overview.",
    ) {
        IntegrityOverviewOutput(overview = getIntegrityOverviewUseCase())
    }
}
