package com.linknest.core.action.action

import android.net.Uri
import com.linknest.core.action.ActionIssue
import com.linknest.core.action.ActionResult
import com.linknest.core.action.AppAction
import com.linknest.core.network.model.NormalizedUrl
import javax.inject.Inject

class ValidateUrlAction @Inject constructor() : AppAction<NormalizedUrl, NormalizedUrl> {

    companion object {
        private const val WARNING_URL_LENGTH = 2048
    }

    override suspend fun invoke(input: NormalizedUrl): ActionResult<NormalizedUrl> = when {
        Uri.parse(input.normalizedUrl).scheme.isNullOrBlank() -> ActionResult.Failure(
            issue = ActionIssue(
                code = "URL_SCHEME_REQUIRED",
                message = "URL must include a scheme.",
            ),
        )

        input.host.isBlank() -> ActionResult.Failure(
            issue = ActionIssue(
                code = "URL_HOST_INVALID",
                message = "URL must include a valid host.",
            ),
        )

        input.normalizedUrl.length > WARNING_URL_LENGTH ||
            input.wasInsecureSchemeUpgraded ||
            input.isInternationalizedHost ||
            input.hasUnsupportedScheme -> {
            val issues = buildList {
                if (input.normalizedUrl.length > WARNING_URL_LENGTH) {
                    add(
                        ActionIssue(
                            code = "URL_LENGTH_WARNING",
                            message = "URL is unusually long and may not work in every app.",
                        ),
                    )
                }
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
                if (input.hasUnsupportedScheme) {
                    add(
                        ActionIssue(
                            code = "URL_SCHEME_UNSUPPORTED_FOR_AUTOMATION",
                            message = "This link can be saved, but LinkNest will not fetch metadata or run health checks for its scheme.",
                        ),
                    )
                }
            }
            ActionResult.PartialSuccess(input, issues)
        }

        else -> ActionResult.Success(input)
    }
}
