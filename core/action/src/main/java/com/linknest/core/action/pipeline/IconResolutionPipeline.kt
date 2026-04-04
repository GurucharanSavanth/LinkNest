package com.linknest.core.action.pipeline

import com.linknest.core.action.ActionIssue
import com.linknest.core.action.ActionResult
import com.linknest.core.action.action.CacheIconAction
import com.linknest.core.action.action.CacheIconInput
import com.linknest.core.action.action.ResolveIconAction
import com.linknest.core.action.model.IconResolutionInput
import com.linknest.core.action.model.IconResolutionOutput
import javax.inject.Inject

class IconResolutionPipeline @Inject constructor(
    private val resolveIconAction: ResolveIconAction,
    private val cacheIconAction: CacheIconAction,
) {
    suspend operator fun invoke(input: IconResolutionInput): ActionResult<IconResolutionOutput> {
        val issues = mutableListOf<ActionIssue>()
        val resolution = when (val result = resolveIconAction(input)) {
            is ActionResult.Success -> result.value
            is ActionResult.PartialSuccess -> {
                issues += result.issues
                result.value
            }
            is ActionResult.Failure -> return result
        }

        if (resolution.sourceUrl.isNullOrBlank()) {
            return if (issues.isEmpty()) {
                ActionResult.Success(resolution)
            } else {
                ActionResult.PartialSuccess(resolution, issues)
            }
        }

        return when (val cacheResult = cacheIconAction(CacheIconInput(resolution.sourceUrl, input.fetchedAt))) {
            is ActionResult.Success -> {
                val output = resolution.copy(persistedIconCache = cacheResult.value)
                if (issues.isEmpty()) ActionResult.Success(output) else ActionResult.PartialSuccess(output, issues)
            }
            is ActionResult.PartialSuccess -> {
                val output = resolution.copy(persistedIconCache = cacheResult.value)
                ActionResult.PartialSuccess(output, issues + cacheResult.issues)
            }
            is ActionResult.Failure -> {
                issues += cacheResult.issue
                ActionResult.PartialSuccess(resolution, issues)
            }
        }
    }
}
