package com.linknest.core.action.action

import com.linknest.core.action.ActionIssue
import com.linknest.core.action.ActionResult
import com.linknest.core.action.AppAction
import com.linknest.core.network.UrlNormalizer
import com.linknest.core.network.model.NormalizedUrl
import javax.inject.Inject

class NormalizeUrlAction @Inject constructor(
    private val urlNormalizer: UrlNormalizer,
) : AppAction<String, NormalizedUrl> {
    override suspend fun invoke(input: String): ActionResult<NormalizedUrl> =
        urlNormalizer.normalize(input).fold(
            onSuccess = { normalizedUrl -> ActionResult.Success(normalizedUrl) },
            onFailure = { throwable ->
                ActionResult.Failure(
                    issue = ActionIssue(
                        code = "INVALID_URL",
                        message = throwable.message ?: "Unable to normalize URL.",
                    ),
                    throwable = throwable,
                )
            },
        )
}
