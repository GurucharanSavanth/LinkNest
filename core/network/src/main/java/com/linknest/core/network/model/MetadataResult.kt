package com.linknest.core.network.model

import com.linknest.core.model.IconSource

data class MetadataResult(
    val title: String,
    val canonicalUrl: String?,
    val finalUrl: String,
    val domain: String,
    val ogImageUrl: String?,
    val faviconUrl: String?,
    val chosenIconSource: IconSource,
)
