package com.linknest.core.network

import com.linknest.core.common.coroutine.IoDispatcher
import com.linknest.core.model.IconSource
import com.linknest.core.network.model.MetadataResult
import com.linknest.core.network.model.NormalizedUrl
import java.net.URI
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class UrlMetadataFetcher @Inject constructor(
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun fetch(normalizedUrl: NormalizedUrl): MetadataResult = withContext(ioDispatcher) {
        try {
            val document = Jsoup.connect(normalizedUrl.normalizedUrl)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MILLIS)
                .followRedirects(true)
                .get()

            val finalUrl = document.location().ifBlank { normalizedUrl.normalizedUrl }
            UrlSecurityPolicy.validateResolvedUrl(finalUrl)
            val resolvedDomain = runCatching {
                URI(finalUrl).host?.trim()?.let { host -> UrlSecurityPolicy.normalizeHost(host).first }?.removePrefix("www.")
            }.getOrNull().orEmpty().ifBlank { normalizedUrl.domain }

            val ogImageUrl = firstResolvedMetaContent(
                document,
                "meta[property=og:image],meta[property='og:image:url'],meta[name=twitter:image]",
            )

            val iconLink = findPreferredRelIcon(document)
            val fallbackFavicon = runCatching { URI(finalUrl).resolve("/favicon.ico").toString() }.getOrNull()

            val chosenSource = when {
                !ogImageUrl.isNullOrBlank() -> IconSource.OG_IMAGE
                iconLink != null -> iconLink.source
                !fallbackFavicon.isNullOrBlank() -> IconSource.FAVICON_FALLBACK
                else -> IconSource.GENERATED
            }

            MetadataResult(
                title = document.ogTitleOrPageTitle(defaultTitle = resolvedDomain),
                canonicalUrl = document.select("link[rel=canonical]").firstOrNull()?.absUrl("href")?.ifBlank { null },
                finalUrl = finalUrl,
                domain = resolvedDomain,
                ogImageUrl = ogImageUrl,
                faviconUrl = iconLink?.url ?: fallbackFavicon,
                chosenIconSource = chosenSource,
            )
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Throwable) {
            MetadataResult(
                title = normalizedUrl.domain,
                canonicalUrl = null,
                finalUrl = normalizedUrl.normalizedUrl,
                domain = normalizedUrl.domain,
                ogImageUrl = null,
                faviconUrl = null,
                chosenIconSource = IconSource.GENERATED,
            )
        }
    }

    private fun Document.ogTitleOrPageTitle(defaultTitle: String): String {
        val ogTitle = select("meta[property=og:title],meta[name=twitter:title]")
            .firstOrNull()
            ?.attr("content")
            ?.trim()
            .orEmpty()
        if (ogTitle.isNotBlank()) return ogTitle
        val pageTitle = title().trim()
        return if (pageTitle.isNotBlank()) pageTitle else defaultTitle
    }

    private fun firstResolvedMetaContent(
        document: Document,
        selector: String,
    ): String? = document.select(selector)
        .firstOrNull()
        ?.absUrl("content")
        ?.ifBlank { null }

    private fun findPreferredRelIcon(document: Document): IconLink? {
        val candidates = document.select("link[rel]")
            .mapNotNull(::toIconLink)

        val appleTouch = candidates.firstOrNull { it.source == IconSource.APPLE_TOUCH_ICON }
        if (appleTouch != null) return appleTouch

        return candidates.firstOrNull()
    }

    private fun toIconLink(element: Element): IconLink? {
        val rel = element.attr("rel").lowercase()
        val href = element.absUrl("href").ifBlank { null } ?: return null
        return when {
            "apple-touch-icon" in rel -> IconLink(href, IconSource.APPLE_TOUCH_ICON)
            rel.contains("icon") -> IconLink(href, IconSource.REL_ICON)
            else -> null
        }
    }

    private data class IconLink(
        val url: String,
        val source: IconSource,
    )

    private companion object {
        const val TIMEOUT_MILLIS = 12_000
        const val USER_AGENT =
            "Mozilla/5.0 (Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36 LinkNest/0.1"
    }
}
