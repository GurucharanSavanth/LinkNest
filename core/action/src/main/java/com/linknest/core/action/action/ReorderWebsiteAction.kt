package com.linknest.core.action.action

import com.linknest.core.action.ActionResult
import com.linknest.core.action.AppAction
import com.linknest.core.action.actionResult
import com.linknest.core.action.model.ReorderWebsiteInput
import com.linknest.core.data.usecase.ReorderWebsiteUseCase
import javax.inject.Inject

class ReorderWebsiteAction @Inject constructor(
    private val reorderWebsiteUseCase: ReorderWebsiteUseCase,
) : AppAction<ReorderWebsiteInput, Unit> {
    override suspend fun invoke(input: ReorderWebsiteInput): ActionResult<Unit> = actionResult(
        code = "REORDER_WEBSITE_FAILED",
        defaultMessage = "Unable to reorder websites.",
    ) {
        reorderWebsiteUseCase(
            categoryId = input.categoryId,
            orderedIds = input.orderedIds,
        )
    }
}
