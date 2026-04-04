package com.linknest.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink

const val SETTINGS_ROUTE = "settings"
const val INTEGRITY_CENTER_ROUTE = "integrity-center"
const val HEALTH_REPORT_ROUTE = "health-report"
const val HEALTH_REPORT_AUTO_RUN_ARG = "autorun"
private const val HEALTH_REPORT_ROUTE_PATTERN =
    "$HEALTH_REPORT_ROUTE?$HEALTH_REPORT_AUTO_RUN_ARG={$HEALTH_REPORT_AUTO_RUN_ARG}"

fun NavController.navigateToSettings() {
    navigate(SETTINGS_ROUTE)
}

fun NavController.navigateToIntegrityCenter() {
    navigate(INTEGRITY_CENTER_ROUTE)
}

fun NavController.navigateToHealthReport(autoRun: Boolean = false) {
    val route = if (autoRun) {
        "$HEALTH_REPORT_ROUTE?$HEALTH_REPORT_AUTO_RUN_ARG=true"
    } else {
        HEALTH_REPORT_ROUTE
    }
    navigate(route)
}

fun NavGraphBuilder.settingsScreen(
    onBack: () -> Unit,
    onOpenIntegrityCenter: () -> Unit,
    onOpenHealthReport: () -> Unit,
) {
    composable(
        route = SETTINGS_ROUTE,
        deepLinks = listOf(
            navDeepLink { uriPattern = "linknest://settings" },
        ),
    ) {
        SettingsRoute(
            onBack = onBack,
            onOpenIntegrityCenter = onOpenIntegrityCenter,
            onOpenHealthReport = onOpenHealthReport,
        )
    }

    composable(
        route = INTEGRITY_CENTER_ROUTE,
        deepLinks = listOf(
            navDeepLink { uriPattern = "linknest://integrity" },
        ),
    ) {
        IntegrityCenterRoute(
            onBack = onBack,
            onOpenHealthReport = onOpenHealthReport,
        )
    }

    composable(
        route = HEALTH_REPORT_ROUTE_PATTERN,
        arguments = listOf(
            navArgument(HEALTH_REPORT_AUTO_RUN_ARG) {
                type = NavType.BoolType
                defaultValue = false
            },
        ),
        deepLinks = listOf(
            navDeepLink { uriPattern = "linknest://health-report" },
            navDeepLink { uriPattern = "linknest://health-report?$HEALTH_REPORT_AUTO_RUN_ARG={$HEALTH_REPORT_AUTO_RUN_ARG}" },
        ),
    ) {
        HealthReportRoute(
            onBack = onBack,
        )
    }
}
