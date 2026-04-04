package com.linknest.core.network.model

data class NormalizedUrl(
    val rawInput: String,
    val normalizedUrl: String,
    val host: String,
    val domain: String,
    val wasInsecureSchemeUpgraded: Boolean = false,
    val isInternationalizedHost: Boolean = false,
)
