package com.linknest.core.action.action

import com.linknest.core.action.ActionResult
import com.linknest.core.action.AppAction
import com.linknest.core.action.actionResult
import com.linknest.core.network.UrlMetadataFetcher
import com.linknest.core.network.model.MetadataResult
import com.linknest.core.network.model.NormalizedUrl
import javax.inject.Inject

class FetchMetadataAction @Inject constructor(
    private val metadataFetcher: UrlMetadataFetcher,
) : AppAction<NormalizedUrl, MetadataResult> {
    override suspend fun invoke(input: NormalizedUrl): ActionResult<MetadataResult> = actionResult(
        code = "METADATA_FETCH_FAILED",
        defaultMessage = "Unable to fetch website metadata.",
    ) {
        metadataFetcher.fetch(input)
    }
}
