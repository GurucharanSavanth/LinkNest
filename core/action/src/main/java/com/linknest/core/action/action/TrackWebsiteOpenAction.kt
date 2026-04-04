package com.linknest.core.action.action

import com.linknest.core.action.ActionResult
import com.linknest.core.action.AppAction
import com.linknest.core.action.actionResult
import com.linknest.core.data.usecase.TrackWebsiteOpenUseCase
import javax.inject.Inject

class TrackWebsiteOpenAction @Inject constructor(
    private val trackWebsiteOpenUseCase: TrackWebsiteOpenUseCase,
) : AppAction<Long, Unit> {
    override suspend fun invoke(input: Long): ActionResult<Unit> = actionResult(
        code = "TRACK_OPEN_FAILED",
        defaultMessage = "Unable to record website open.",
    ) {
        trackWebsiteOpenUseCase(input)
    }
}
