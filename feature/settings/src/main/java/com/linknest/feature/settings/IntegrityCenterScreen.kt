package com.linknest.feature.settings

import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linknest.core.designsystem.component.GlassPanel
import com.linknest.core.designsystem.component.LinkNestGradientBackground
import com.linknest.core.model.IntegrityEvent
import com.linknest.core.model.IntegrityEventType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

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

@androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PRIVATE)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrityCenterScreen(
    uiState: IntegrityCenterUiState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenHealthReport: () -> Unit,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val numberFormat = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    LinkNestGradientBackground(modifier = modifier) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_integrity_center)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.settings_go_back))
                        }
                    },
                    actions = {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.settings_refresh_diagnostics))
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
                                text = stringResource(R.string.settings_diagnostics_overview),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            IntegrityMetricRow(stringResource(R.string.settings_last_health_check), uiState.overview.lastHealthCheckAt.formatTimestamp())
                            IntegrityMetricRow(stringResource(R.string.settings_last_backup), uiState.overview.lastBackupAt.formatTimestamp())
                            IntegrityMetricRow(stringResource(R.string.settings_last_restore), uiState.overview.lastRestoreAt.formatTimestamp())
                            IntegrityMetricRow(stringResource(R.string.settings_broken_links), numberFormat.format(uiState.overview.brokenLinksCount))
                            IntegrityMetricRow(stringResource(R.string.settings_possible_duplicates), numberFormat.format(uiState.overview.duplicateCount))
                            IntegrityMetricRow(stringResource(R.string.settings_unsorted_entries), numberFormat.format(uiState.overview.unsortedCount))
                            IntegrityMetricRow(stringResource(R.string.settings_icon_cache_size), Formatter.formatShortFileSize(context, uiState.overview.cacheSizeBytes))
                            IntegrityMetricRow(stringResource(R.string.settings_database_size), Formatter.formatShortFileSize(context, uiState.overview.databaseSizeBytes))
                        }
                    }

                    item {
                        GlassPanel {
                            Text(
                                text = stringResource(R.string.settings_maintenance),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = onOpenHealthReport) {
                                    Icon(Icons.Rounded.HealthAndSafety, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.settings_open_health_report))
                                }
                                OutlinedButton(onClick = onRefresh) {
                                    Text(stringResource(R.string.settings_refresh))
                                }
                            }
                        }
                    }

                    if (uiState.overview.recentEvents.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.settings_recent_integrity_events),
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
                                        text = stringResource(event.type.labelResource),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntegrityMetricRow(
    title: String,
    value: String,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = title, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontWeight = FontWeight.Medium)
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
internal fun Long?.formatTimestamp(): String {
    if (this == null || this <= 0L) return stringResource(R.string.settings_not_available)
    val locale = LocalConfiguration.current.locales[0]
    val use24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val zone = TimeZone.getDefault()
    val formatter = remember(locale, use24Hour, zone) {
        val pattern = DateFormat.getBestDateTimePattern(locale, if (use24Hour) "yMMMdHm" else "yMMMdhm")
        SimpleDateFormat(pattern, locale).apply { timeZone = zone }
    }
    return formatter.format(Date(this))
}

@get:androidx.annotation.StringRes
private val IntegrityEventType.labelResource: Int
    get() = when (this) {
        IntegrityEventType.HEALTH_SCAN -> R.string.settings_event_health_scan
        IntegrityEventType.BACKUP_EXPORT -> R.string.settings_event_backup_export
        IntegrityEventType.RESTORE_IMPORT -> R.string.settings_event_restore_import
        IntegrityEventType.DUPLICATE_SCAN -> R.string.settings_event_duplicate_scan
        IntegrityEventType.CACHE_MAINTENANCE -> R.string.settings_event_cache_maintenance
    }
