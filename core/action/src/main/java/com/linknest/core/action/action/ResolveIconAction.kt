package com.linknest.core.action.action

import com.linknest.core.action.ActionResult
import com.linknest.core.action.AppAction
import com.linknest.core.action.model.IconResolutionInput
import com.linknest.core.action.model.IconResolutionOutput
import com.linknest.core.model.IconSource
import com.linknest.core.network.UrlSecurityPolicy
import java.net.URL
import java.util.Locale
import javax.inject.Inject

class ResolveIconAction @Inject constructor() : AppAction<IconResolutionInput, IconResolutionOutput> {
    override suspend fun invoke(input: IconResolutionInput): ActionResult<IconResolutionOutput> {
        val resolved = when {
            !input.customIconUri.isNullOrBlank() && isRemoteUrl(input.customIconUri) -> IconResolutionOutput(
                chosenIconSource = IconSource.CUSTOM,
                sourceUrl = input.customIconUri,
                persistedIconCache = null,
            )

            !input.emojiIcon.isNullOrBlank() -> IconResolutionOutput(
                chosenIconSource = IconSource.EMOJI,
                sourceUrl = null,
                persistedIconCache = null,
            )

            input.preferredSource == IconSource.OG_IMAGE && isRemoteUrl(input.ogImageUrl) -> IconResolutionOutput(
                chosenIconSource = IconSource.OG_IMAGE,
                sourceUrl = input.ogImageUrl,
                persistedIconCache = null,
            )

            isRemoteUrl(input.faviconUrl) -> IconResolutionOutput(
                chosenIconSource = when (input.preferredSource) {
                    IconSource.APPLE_TOUCH_ICON,
                    IconSource.REL_ICON,
                    IconSource.FAVICON_FALLBACK,
                    -> input.preferredSource
                    else -> IconSource.FAVICON_FALLBACK
                },
                sourceUrl = input.faviconUrl,
                persistedIconCache = null,
            )

            else -> IconResolutionOutput(
                chosenIconSource = IconSource.GENERATED,
                sourceUrl = null,
                persistedIconCache = null,
            )
        }

        return ActionResult.Success(resolved)
    }

    private fun isRemoteUrl(url: String?): Boolean = runCatching {
        val parsed = URL(url)
        val scheme = parsed.protocol?.lowercase(Locale.US)
        val host = parsed.host?.takeIf(String::isNotBlank)
        (scheme == "http" || scheme == "https") &&
            host != null &&
            UrlSecurityPolicy.isHostAllowed(host)
    }.getOrDefault(false)
}
