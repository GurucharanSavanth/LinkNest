package com.linknest.feature.addedit

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink

const val ADD_EDIT_BASE_ROUTE = "addedit"
const val ADD_EDIT_URL_ARG = "url"
const val ADD_EDIT_WEBSITE_ID_ARG = "websiteId"
const val ADD_EDIT_ROUTE =
    "$ADD_EDIT_BASE_ROUTE?$ADD_EDIT_URL_ARG={$ADD_EDIT_URL_ARG}&$ADD_EDIT_WEBSITE_ID_ARG={$ADD_EDIT_WEBSITE_ID_ARG}"

fun NavController.navigateToAddEdit(
    prefillUrl: String? = null,
    websiteId: Long? = null,
) {
    val queryParams = buildList {
        if (!prefillUrl.isNullOrBlank()) {
            add("$ADD_EDIT_URL_ARG=${Uri.encode(prefillUrl)}")
        }
        if (websiteId != null) {
            add("$ADD_EDIT_WEBSITE_ID_ARG=$websiteId")
        }
    }
    val destination = if (queryParams.isEmpty()) {
        ADD_EDIT_BASE_ROUTE
    } else {
        "$ADD_EDIT_BASE_ROUTE?${queryParams.joinToString("&")}"
    }
    navigate(destination)
}

fun NavGraphBuilder.addEditScreen(
    onDone: () -> Unit,
) {
    composable(
        route = ADD_EDIT_ROUTE,
        arguments = listOf(
            navArgument(ADD_EDIT_URL_ARG) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument(ADD_EDIT_WEBSITE_ID_ARG) {
                type = NavType.LongType
                defaultValue = -1L
            },
        ),
        deepLinks = listOf(
            navDeepLink { uriPattern = "linknest://add?$ADD_EDIT_URL_ARG={$ADD_EDIT_URL_ARG}" },
            navDeepLink { uriPattern = "linknest://website-edit/{$ADD_EDIT_WEBSITE_ID_ARG}" },
        ),
    ) {
        AddEditRoute(onDone = onDone)
    }
}
