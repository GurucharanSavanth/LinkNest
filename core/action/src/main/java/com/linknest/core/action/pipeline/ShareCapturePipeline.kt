package com.linknest.core.action.pipeline

import com.linknest.core.action.ActionResult
import com.linknest.core.action.action.HandleSharedUrlAction
import com.linknest.core.action.model.ShareCapturePipelineInput
import com.linknest.core.action.model.ShareCapturePipelineOutput
import javax.inject.Inject

class ShareCapturePipeline @Inject constructor(
    private val handleSharedUrlAction: HandleSharedUrlAction,
) {
    suspend operator fun invoke(
        input: ShareCapturePipelineInput,
    ): ActionResult<ShareCapturePipelineOutput> = when (val result = handleSharedUrlAction(input.sharedText)) {
        is ActionResult.Success -> ActionResult.Success(
            ShareCapturePipelineOutput(
                normalizedUrl = result.value.normalizedUrl,
            ),
        )
        is ActionResult.PartialSuccess -> ActionResult.PartialSuccess(
            value = ShareCapturePipelineOutput(
                normalizedUrl = result.value.normalizedUrl,
            ),
            issues = result.issues,
        )
        is ActionResult.Failure -> result
    }
}
