package com.linknest.core.action.action

import com.linknest.core.action.ActionResult
import com.linknest.core.action.AppAction
import com.linknest.core.action.actionResult
import com.linknest.core.data.usecase.ReorderCategoryUseCase
import javax.inject.Inject

class ReorderCategoryAction @Inject constructor(
    private val reorderCategoryUseCase: ReorderCategoryUseCase,
) : AppAction<List<Long>, Unit> {
    override suspend fun invoke(input: List<Long>): ActionResult<Unit> = actionResult(
        code = "REORDER_CATEGORY_FAILED",
        defaultMessage = "Unable to reorder categories.",
    ) {
        reorderCategoryUseCase(input)
    }
}
