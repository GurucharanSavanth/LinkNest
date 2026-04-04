package com.linknest.feature.dashboard

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink

const val DASHBOARD_ROUTE = "dashboard"
const val DASHBOARD_CATEGORY_ID_ARG = "categoryId"
const val DASHBOARD_WEBSITE_ID_ARG = "websiteId"
private const val DASHBOARD_ROUTE_PATTERN =
    "$DASHBOARD_ROUTE?$DASHBOARD_CATEGORY_ID_ARG={$DASHBOARD_CATEGORY_ID_ARG}&$DASHBOARD_WEBSITE_ID_ARG={$DASHBOARD_WEBSITE_ID_ARG}"

fun NavController.navigateToDashboard(
    categoryId: Long? = null,
    websiteId: Long? = null,
    navOptions: NavOptions? = null,
) {
    val route = buildString {
        append(DASHBOARD_ROUTE)
        val queryParts = buildList {
            if (categoryId != null) add("$DASHBOARD_CATEGORY_ID_ARG=$categoryId")
            if (websiteId != null) add("$DASHBOARD_WEBSITE_ID_ARG=$websiteId")
        }
        if (queryParts.isNotEmpty()) {
            append('?')
            append(queryParts.joinToString("&"))
        }
    }
    navigate(route = route, navOptions = navOptions)
}

fun NavGraphBuilder.dashboardScreen(
    onAddWebsite: () -> Unit,
    onEditWebsite: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHealthReport: () -> Unit,
    onOpenWebsite: (Long, String) -> Unit,
) {
    composable(
        route = DASHBOARD_ROUTE_PATTERN,
        arguments = listOf(
            navArgument(DASHBOARD_CATEGORY_ID_ARG) {
                type = NavType.LongType
                defaultValue = -1L
            },
            navArgument(DASHBOARD_WEBSITE_ID_ARG) {
                type = NavType.LongType
                defaultValue = -1L
            },
        ),
        deepLinks = listOf(
            navDeepLink { uriPattern = "linknest://dashboard" },
            navDeepLink { uriPattern = "linknest://category/{$DASHBOARD_CATEGORY_ID_ARG}" },
            navDeepLink { uriPattern = "linknest://website/{$DASHBOARD_WEBSITE_ID_ARG}" },
        ),
    ) {
        DashboardRoute(
            onAddWebsite = onAddWebsite,
            onEditWebsite = onEditWebsite,
            onOpenSearch = onOpenSearch,
            onOpenSettings = onOpenSettings,
            onOpenHealthReport = onOpenHealthReport,
            onOpenWebsite = onOpenWebsite,
        )
    }
}
