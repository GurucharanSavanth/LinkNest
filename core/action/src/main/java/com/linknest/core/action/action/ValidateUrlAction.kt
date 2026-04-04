package com.linknest.core.action.action

import android.net.Uri
import com.linknest.core.action.ActionIssue
import com.linknest.core.action.ActionResult
import com.linknest.core.action.AppAction
import com.linknest.core.network.model.NormalizedUrl
import javax.inject.Inject

class ValidateUrlAction @Inject constructor() : AppAction<NormalizedUrl, NormalizedUrl> {

    companion object {
        private val ALLOWED_SCHEMES = setOf("http", "https")
    }

    override suspend fun invoke(input: NormalizedUrl): ActionResult<NormalizedUrl> = when {
        Uri.parse(input.normalizedUrl).scheme?.lowercase() !in ALLOWED_SCHEMES -> ActionResult.Failure(
            issue = ActionIssue(
                code = "URL_SCHEME_NOT_ALLOWED",
                message = "Only HTTP and HTTPS URLs are supported.",
            ),
        )

        input.normalizedUrl.length > 2048 -> ActionResult.Failure(
            issue = ActionIssue(
                code = "URL_TOO_LONG",
                message = "URL exceeds the supported length.",
            ),
        )

        input.host.isBlank() -> ActionResult.Failure(
            issue = ActionIssue(
                code = "URL_HOST_INVALID",
                message = "URL must include a valid host.",
            ),
        )

        input.wasInsecureSchemeUpgraded || input.isInternationalizedHost -> {
            val issues = buildList {
                if (input.wasInsecureSchemeUpgraded) {
                    add(
                        ActionIssue(
                            code = "URL_UPGRADED_TO_HTTPS",
                            message = "Insecure HTTP input was upgraded to HTTPS.",
                        ),
                    )
                }
                if (input.isInternationalizedHost) {
                    add(
                        ActionIssue(
                            code = "IDN_HOST_NORMALIZED",
                            message = "Internationalized domain was normalized to a punycode-safe host.",
                        ),
                    )
                }
            }
            ActionResult.PartialSuccess(input, issues)
        }

        else -> ActionResult.Success(input)
    }
}
