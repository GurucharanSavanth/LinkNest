package com.linknest.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linknest.core.designsystem.component.GlassPanel
import com.linknest.core.designsystem.component.LinkNestGradientBackground
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

@OptIn(ExperimentalMaterial3Api::class)
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
                    title = { Text("Health Report") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Go back")
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
                            text = "Legend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                        LegendRow(Color(0xFF4CAF50), "Green = Good health")
                        LegendRow(Color(0xFFFFC107), "Yellow = Blocked / use VPN / login / caution")
                        LegendRow(MaterialTheme.colorScheme.error, "Red = Not good health / dead / unreachable")
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = onRunHealthCheck, enabled = !uiState.isRunning) {
                                if (uiState.isRunning) {
                                    CircularProgressIndicator(strokeWidth = 2.dp)
                                } else {
                                    Text("Run Health Check")
                                }
                            }
                            OutlinedButton(onClick = onRefresh, enabled = !uiState.isRunning) {
                                Text("Refresh Report")
                            }
                        }
                        uiState.summary?.let { summary ->
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = summary,
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
                                    text = item.status.userFacingLabel,
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

private val HealthStatus.userFacingLabel: String
    get() = when (this) {
        HealthStatus.OK -> "Good"
        HealthStatus.BLOCKED -> "Blocked"
        HealthStatus.LOGIN_REQUIRED -> "Login"
        HealthStatus.REDIRECTED -> "Redirected"
        HealthStatus.DNS_FAILED -> "DNS failed"
        HealthStatus.SSL_ISSUE -> "TLS issue"
        HealthStatus.DEAD -> "Dead"
        HealthStatus.TIMEOUT -> "Timeout"
        HealthStatus.UNKNOWN -> "Unknown"
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
