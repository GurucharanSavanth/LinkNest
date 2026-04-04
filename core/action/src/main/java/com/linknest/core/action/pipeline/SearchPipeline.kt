package com.linknest.core.action.pipeline

import com.linknest.core.action.ActionResult
import com.linknest.core.action.action.SearchWebsiteAction
import com.linknest.core.action.model.SearchPipelineInput
import com.linknest.core.action.model.SearchPipelineOutput
import com.linknest.core.model.SearchResultGroup
import com.linknest.core.network.UrlSecurityPolicy
import javax.inject.Inject

class SearchPipeline @Inject constructor(
    private val searchWebsiteAction: SearchWebsiteAction,
) {
    suspend operator fun invoke(input: SearchPipelineInput): ActionResult<SearchPipelineOutput> {
        val sanitizedQuery = UrlSecurityPolicy.sanitizeSearchQuery(input.query)
        if (sanitizedQuery.isBlank()) {
            return ActionResult.Success(
                SearchPipelineOutput(
                    query = "",
                    groups = emptyList(),
                    totalCount = 0,
                ),
            )
        }

        return when (val result = searchWebsiteAction(input.copy(query = sanitizedQuery))) {
            is ActionResult.Success -> {
                val groups = if (input.groupByCategory) {
                    result.value.groupBy { it.categoryName }
                        .map { (categoryName, results) ->
                            SearchResultGroup(
                                title = categoryName,
                                results = results,
                            )
                        }
                        .sortedBy { group -> group.title.lowercase() }
                } else {
                    listOf(SearchResultGroup(title = "Results", results = result.value))
                }
                ActionResult.Success(
                    SearchPipelineOutput(
                        query = sanitizedQuery,
                        groups = groups,
                        totalCount = result.value.size,
                    ),
                )
            }
            is ActionResult.PartialSuccess -> {
                val groups = if (input.groupByCategory) {
                    result.value.groupBy { it.categoryName }
                        .map { (title, results) -> SearchResultGroup(title = title, results = results) }
                        .sortedBy { group -> group.title.lowercase() }
                } else {
                    listOf(SearchResultGroup(title = "Results", results = result.value))
                }
                ActionResult.PartialSuccess(
                    value = SearchPipelineOutput(
                        query = sanitizedQuery,
                        groups = groups,
                        totalCount = result.value.size,
                    ),
                    issues = result.issues,
                )
            }
            is ActionResult.Failure -> result
        }
    }
}
