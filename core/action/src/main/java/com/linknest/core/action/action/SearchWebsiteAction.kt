package com.linknest.core.action.action

import com.linknest.core.action.ActionResult
import com.linknest.core.action.AppAction
import com.linknest.core.action.actionResult
import com.linknest.core.action.model.SearchPipelineInput
import com.linknest.core.data.usecase.SearchUseCase
import com.linknest.core.model.SearchResultItem
import javax.inject.Inject

class SearchWebsiteAction @Inject constructor(
    private val searchUseCase: SearchUseCase,
) : AppAction<SearchPipelineInput, List<SearchResultItem>> {
    override suspend fun invoke(input: SearchPipelineInput): ActionResult<List<SearchResultItem>> = actionResult(
        code = "SEARCH_FAILED",
        defaultMessage = "Unable to search saved websites.",
    ) {
        searchUseCase(
            query = input.query,
            limit = input.limit,
        )
    }
}
