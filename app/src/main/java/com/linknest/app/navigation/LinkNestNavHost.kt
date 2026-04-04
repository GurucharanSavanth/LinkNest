package com.linknest.app.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.linknest.app.launch.LAUNCH_GATE_ROUTE
import com.linknest.app.launch.LaunchGateRoute
import com.linknest.feature.addedit.addEditScreen
import com.linknest.feature.addedit.navigateToAddEdit
import com.linknest.feature.dashboard.DASHBOARD_ROUTE
import com.linknest.feature.dashboard.dashboardScreen
import com.linknest.feature.search.navigateToSearch
import com.linknest.feature.search.searchScreen
import com.linknest.feature.settings.navigateToHealthReport
import com.linknest.feature.settings.navigateToIntegrityCenter
import com.linknest.feature.settings.navigateToSettings
import com.linknest.feature.settings.settingsScreen

@Composable
fun LinkNestNavHost(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = LAUNCH_GATE_ROUTE,
        modifier = modifier,
    ) {
        composable(route = LAUNCH_GATE_ROUTE) {
            LaunchGateRoute(
                onOpenDashboard = {
                    navController.navigate(DASHBOARD_ROUTE) {
                        popUpTo(LAUNCH_GATE_ROUTE) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
            )
        }
        dashboardScreen(
            onAddWebsite = { navController.navigateToAddEdit() },
            onEditWebsite = { websiteId -> navController.navigateToAddEdit(websiteId = websiteId) },
            onOpenSearch = { navController.navigateToSearch() },
            onOpenSettings = { navController.navigateToSettings() },
            onOpenHealthReport = { navController.navigateToHealthReport() },
            onOpenWebsite = { _, url -> openExternalWebsite(context, url) },
        )
        addEditScreen(
            onDone = { navController.popBackStack() },
        )
        searchScreen(
            onBack = { navController.popBackStack() },
            onOpenWebsite = { _, url -> openExternalWebsite(context, url) },
        )
        settingsScreen(
            onBack = { navController.popBackStack() },
            onOpenIntegrityCenter = { navController.navigateToIntegrityCenter() },
            onOpenHealthReport = { navController.navigateToHealthReport() },
        )
    }
}

private fun openExternalWebsite(context: Context, url: String) {
    val parsed = runCatching { Uri.parse(url) }.getOrNull()
    val scheme = parsed?.scheme?.lowercase()
    val host = parsed?.host
    if ((scheme != "http" && scheme != "https") || host.isNullOrBlank()) {
        Toast.makeText(context, "This link cannot be opened", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, parsed).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show()
    }
}
