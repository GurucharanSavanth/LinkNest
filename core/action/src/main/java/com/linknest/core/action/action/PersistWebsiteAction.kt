package com.linknest.core.action.action

import com.linknest.core.action.ActionResult
import com.linknest.core.action.AppAction
import com.linknest.core.action.actionResult
import com.linknest.core.data.model.PersistWebsiteRequest
import com.linknest.core.data.repository.WebsiteRepository
import javax.inject.Inject

class PersistWebsiteAction @Inject constructor(
    private val websiteRepository: WebsiteRepository,
) : AppAction<PersistWebsiteRequest, Long> {
    override suspend fun invoke(input: PersistWebsiteRequest): ActionResult<Long> = actionResult(
        code = "PERSIST_WEBSITE_FAILED",
        defaultMessage = "Unable to save website.",
    ) {
        websiteRepository.addWebsite(input)
    }
}
