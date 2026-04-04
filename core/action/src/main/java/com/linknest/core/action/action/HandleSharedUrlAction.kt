package com.linknest.core.action.action

import com.linknest.core.action.ActionResult
import com.linknest.core.action.AppAction
import com.linknest.core.action.actionResult
import com.linknest.core.data.usecase.HandleIncomingShareUseCase
import com.linknest.core.network.model.NormalizedUrl
import javax.inject.Inject

class HandleSharedUrlAction @Inject constructor(
    private val handleIncomingShareUseCase: HandleIncomingShareUseCase,
) : AppAction<String, NormalizedUrl> {
    override suspend fun invoke(input: String): ActionResult<NormalizedUrl> = actionResult(
        code = "INVALID_SHARED_URL",
        defaultMessage = "No usable website URL was found in the shared content.",
    ) {
        handleIncomingShareUseCase.invoke(input).getOrThrow()
    }
}
