package com.linknest.core.action.action

import com.linknest.core.action.ActionResult
import com.linknest.core.action.AppAction
import com.linknest.core.action.actionResult
import com.linknest.core.data.model.WebsiteHealthCandidate
import com.linknest.core.data.model.WebsiteHealthUpdate
import com.linknest.core.data.usecase.CheckWebsiteHealthUseCase
import javax.inject.Inject

class CheckWebsiteHealthAction @Inject constructor(
    private val checkWebsiteHealthUseCase: CheckWebsiteHealthUseCase,
) : AppAction<WebsiteHealthCandidate, WebsiteHealthUpdate> {
    override suspend fun invoke(input: WebsiteHealthCandidate): ActionResult<WebsiteHealthUpdate> = actionResult(
        code = "HEALTH_CHECK_FAILED",
        defaultMessage = "Unable to check website health.",
    ) {
        checkWebsiteHealthUseCase(input)
    }
}
