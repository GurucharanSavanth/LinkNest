package com.linknest.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linknest.core.designsystem.component.GlassPanel
import com.linknest.core.designsystem.component.LinkNestGradientBackground
import com.linknest.core.designsystem.labelResource
import com.linknest.core.model.HealthReportItem
import com.linknest.core.model.HealthStatus

@Composable
fun HealthReportRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HealthReportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onMessageConsumed()
        }
    }

    HealthReportScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        onBack = onBack,
        onRefresh = viewModel::refreshStoredReport,
        onRunHealthCheck = viewModel::runHealthCheck,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HealthReportScreen(
    uiState: HealthReportUiState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRunHealthCheck: () -> Unit,
) {
    LinkNestGradientBackground(modifier = modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_health_report)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.settings_go_back))
                        }
                    },
                )
            },
        ) { innerPadding ->
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
                            text = stringResource(R.string.settings_legend),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                        LegendRow(Color(0xFF4CAF50), stringResource(R.string.settings_good_health_legend))
                        LegendRow(Color(0xFFFFC107), stringResource(R.string.settings_caution_legend))
                        LegendRow(MaterialTheme.colorScheme.error, stringResource(R.string.settings_failed_health_legend))
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onRunHealthCheck, enabled = !uiState.isRunning) {
                                if (uiState.isRunning) {
                                    CircularProgressIndicator(strokeWidth = 2.dp)
                                } else {
                                    Text(stringResource(R.string.settings_run_health_check))
                                }
                            }
                            OutlinedButton(onClick = onRefresh, enabled = !uiState.isRunning) {
                                Text(stringResource(R.string.settings_refresh_report))
                            }
                        }
                        uiState.summary?.let { summary ->
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(
                                    R.string.settings_health_result,
                                    pluralStringResource(R.plurals.settings_checked_links, summary.checkedCount, summary.checkedCount),
                                    summary.okCount,
                                    summary.blockedCount + summary.redirectedCount + summary.loginRequiredCount +
                                        summary.dnsFailedCount + summary.sslIssueCount,
                                    summary.deadCount + summary.timeoutCount,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    items(uiState.items, key = HealthReportItem::websiteId) { item ->
                        GlassPanel {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.title, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = item.normalizedUrl,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    item.detailMessage?.let { detail ->
                                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = detail,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Text(
                                    text = stringResource(item.status.labelResource),
                                    color = item.status.userFacingColor(MaterialTheme.colorScheme.error),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HealthStatus.userFacingColor(errorColor: Color): Color = when (this) {
    HealthStatus.OK -> Color(0xFF4CAF50)
    HealthStatus.BLOCKED,
    HealthStatus.LOGIN_REQUIRED,
    HealthStatus.REDIRECTED,
    HealthStatus.DNS_FAILED,
    HealthStatus.SSL_ISSUE,
    -> Color(0xFFFFC107)
    HealthStatus.DEAD,
    HealthStatus.TIMEOUT,
    -> errorColor
    HealthStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
}
