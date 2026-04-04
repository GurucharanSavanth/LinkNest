package com.linknest.feature.settings

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linknest.core.designsystem.component.GlassPanel
import com.linknest.core.designsystem.component.LinkNestGradientBackground
import com.linknest.core.model.IntegrityEvent
import java.util.Date

@Composable
fun IntegrityCenterRoute(
    onBack: () -> Unit,
    onOpenHealthReport: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IntegrityCenterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onMessageConsumed()
        }
    }

    IntegrityCenterScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onOpenHealthReport = onOpenHealthReport,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntegrityCenterScreen(
    uiState: IntegrityCenterUiState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenHealthReport: () -> Unit,
) {
    val context = LocalContext.current
    LinkNestGradientBackground(modifier = modifier) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Integrity Center") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Go back")
                        }
                    },
                    actions = {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh diagnostics")
                        }
                    },
                )
            },
        ) { innerPadding ->
            if (uiState.isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        GlassPanel {
                            Text(
                                text = "Diagnostics Overview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            IntegrityMetricRow("Last health check", uiState.overview.lastHealthCheckAt.formatTimestamp())
                            IntegrityMetricRow("Last backup", uiState.overview.lastBackupAt.formatTimestamp())
                            IntegrityMetricRow("Last restore", uiState.overview.lastRestoreAt.formatTimestamp())
                            IntegrityMetricRow("Broken links", uiState.overview.brokenLinksCount.toString())
                            IntegrityMetricRow("Possible duplicates", uiState.overview.duplicateCount.toString())
                            IntegrityMetricRow("Unsorted entries", uiState.overview.unsortedCount.toString())
                            IntegrityMetricRow("Icon cache size", Formatter.formatShortFileSize(context, uiState.overview.cacheSizeBytes))
                            IntegrityMetricRow("Database size", Formatter.formatShortFileSize(context, uiState.overview.databaseSizeBytes))
                        }
                    }

                    item {
                        GlassPanel {
                            Text(
                                text = "Maintenance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = onOpenHealthReport) {
                                    Icon(Icons.Rounded.HealthAndSafety, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open Health Report")
                                }
                                OutlinedButton(onClick = onRefresh) {
                                    Text("Refresh")
                                }
                            }
                        }
                    }

                    if (uiState.overview.recentEvents.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recent Integrity Events",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        items(uiState.overview.recentEvents, key = IntegrityEvent::id) { event ->
                            GlassPanel {
                                Text(text = event.title, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = event.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = event.type.name.lowercase().replace('_', ' '),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                    Text(
                                        text = event.createdAt.formatTimestamp(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntegrityMetricRow(
    title: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontWeight = FontWeight.Medium)
    }
    Spacer(modifier = Modifier.height(8.dp))
}

private fun Long?.formatTimestamp(): String {
    if (this == null || this <= 0L) return "Not available"
    return java.text.DateFormat.getDateTimeInstance(
        java.text.DateFormat.MEDIUM,
        java.text.DateFormat.SHORT,
    )
        .format(Date(this))
}
