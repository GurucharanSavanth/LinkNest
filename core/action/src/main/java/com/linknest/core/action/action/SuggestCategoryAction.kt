package com.linknest.core.action.action

import com.linknest.core.action.ActionResult
import com.linknest.core.action.AppAction
import com.linknest.core.action.actionResult
import com.linknest.core.action.model.SuggestCategoryInput
import com.linknest.core.data.usecase.SuggestCategoryUseCase
import com.linknest.core.model.CategorySuggestion
import javax.inject.Inject

class SuggestCategoryAction @Inject constructor(
    private val suggestCategoryUseCase: SuggestCategoryUseCase,
) : AppAction<SuggestCategoryInput, CategorySuggestion?> {
    override suspend fun invoke(input: SuggestCategoryInput): ActionResult<CategorySuggestion?> = actionResult(
        code = "CATEGORY_SUGGESTION_FAILED",
        defaultMessage = "Unable to suggest a category.",
    ) {
        suggestCategoryUseCase(
            domain = input.domain,
            contextHint = input.contextHint,
        )
    }
}
