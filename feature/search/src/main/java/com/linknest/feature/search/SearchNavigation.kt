package com.linknest.feature.search

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink

const val SEARCH_ROUTE = "search"
const val SEARCH_QUERY_ARG = "query"
const val SEARCH_FILTER_ID_ARG = "filterId"
const val SEARCH_COLLECTION_ARG = "collection"
private const val SEARCH_ROUTE_PATTERN =
    "$SEARCH_ROUTE?$SEARCH_QUERY_ARG={$SEARCH_QUERY_ARG}&$SEARCH_FILTER_ID_ARG={$SEARCH_FILTER_ID_ARG}&$SEARCH_COLLECTION_ARG={$SEARCH_COLLECTION_ARG}"

fun NavController.navigateToSearch(
    query: String? = null,
    filterId: Long? = null,
    collection: SearchSmartCollection? = null,
    navOptions: NavOptions? = null,
) {
    val queryParams = buildList {
        if (!query.isNullOrBlank()) {
            add("$SEARCH_QUERY_ARG=${Uri.encode(query)}")
        }
        if (filterId != null) {
            add("$SEARCH_FILTER_ID_ARG=$filterId")
        }
        if (collection != null) {
            add("$SEARCH_COLLECTION_ARG=${collection.routeValue}")
        }
    }
    val route = if (queryParams.isEmpty()) SEARCH_ROUTE else "$SEARCH_ROUTE?${queryParams.joinToString("&")}"
    navigate(route, navOptions)
}

fun NavGraphBuilder.searchScreen(
    onBack: () -> Unit,
    onOpenWebsite: (Long, String) -> Unit,
) {
    composable(
        route = SEARCH_ROUTE_PATTERN,
        arguments = listOf(
            navArgument(SEARCH_QUERY_ARG) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument(SEARCH_FILTER_ID_ARG) {
                type = NavType.LongType
                defaultValue = -1L
            },
            navArgument(SEARCH_COLLECTION_ARG) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
        deepLinks = listOf(
            navDeepLink { uriPattern = "linknest://search" },
            navDeepLink { uriPattern = "linknest://search?$SEARCH_QUERY_ARG={$SEARCH_QUERY_ARG}" },
            navDeepLink { uriPattern = "linknest://search?$SEARCH_COLLECTION_ARG={$SEARCH_COLLECTION_ARG}" },
            navDeepLink { uriPattern = "linknest://saved-filter/{$SEARCH_FILTER_ID_ARG}" },
        ),
    ) {
        SearchRoute(
            onBack = onBack,
            onOpenWebsite = onOpenWebsite,
        )
    }
}
