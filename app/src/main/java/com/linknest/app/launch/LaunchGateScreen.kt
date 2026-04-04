package com.linknest.app.launch

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.VpnLock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linknest.core.designsystem.component.GlassPanel
import com.linknest.core.designsystem.component.LinkNestGradientBackground
import com.linknest.core.designsystem.R as DesignSystemR
import com.linknest.core.model.HealthReportItem
import com.linknest.core.model.HealthStatus

const val LAUNCH_GATE_ROUTE = "launch_gate"

@Composable
fun LaunchGateRoute(
    onOpenDashboard: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LaunchGateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onMessageConsumed()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToDashboard.collect {
            onOpenDashboard()
        }
    }

    LaunchGateScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        onRunHealthCheck = viewModel::onRunHealthCheck,
        onSkipToDashboard = viewModel::onSkipToDashboard,
        onOpenDashboard = onOpenDashboard,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LaunchGateScreen(
    uiState: LaunchGateUiState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onRunHealthCheck: () -> Unit,
    onSkipToDashboard: () -> Unit,
    onOpenDashboard: () -> Unit,
) {
    LinkNestGradientBackground(modifier = modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    uiState.report != null -> {
                        HealthReportContent(
                            items = uiState.report.items,
                            onOpenDashboard = onOpenDashboard,
                        )
                    }

                    uiState.isChecking -> {
                        HealthCheckProgressContent(
                            uiState = uiState,
                            onSkipToDashboard = onSkipToDashboard,
                        )
                    }

                    else -> {
                        LaunchDecisionContent(
                            onRunHealthCheck = onRunHealthCheck,
                            onSkipToDashboard = onSkipToDashboard,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LaunchDecisionContent(
    onRunHealthCheck: () -> Unit,
    onSkipToDashboard: () -> Unit,
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = DesignSystemR.drawable.ic_linknest_mark),
                    contentDescription = "LinkNest logo",
                    modifier = Modifier.size(56.dp),
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "LinkNest",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Choose whether to verify every saved website before opening the dashboard.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(22.dp))
            Button(
                onClick = onRunHealthCheck,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.HealthAndSafety, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Check Health of all websites")
            }
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(
                onClick = onSkipToDashboard,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Skip and get into the dashboard")
            }
        }
    }
}

@Composable
private fun HealthCheckProgressContent(
    uiState: LaunchGateUiState,
    onSkipToDashboard: () -> Unit,
) {
    val progress = uiState.progress
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Checking website health",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = progress?.latestItem?.normalizedUrl ?: "Preparing checks...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(18.dp))
            LinearProgressIndicator(
                progress = {
                    if (progress == null || progress.totalCount == 0) {
                        0f
                    } else {
                        progress.completedCount.toFloat() / progress.totalCount.toFloat()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (progress == null) {
                    "Preparing..."
                } else {
                    "${progress.completedCount} of ${progress.totalCount} checked"
                },
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onSkipToDashboard) {
                Text("Cancel and open dashboard")
            }
        }
    }
}

@Composable
private fun HealthReportContent(
    items: List<HealthReportItem>,
    onOpenDashboard: () -> Unit,
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Health Report",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Green = good health, Yellow = blocked or VPN required, Red = not good health.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            LegendRow()
            Spacer(modifier = Modifier.height(14.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items, key = { item -> item.websiteId }) { item ->
                    HealthReportRow(item = item)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onOpenDashboard,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Go to Dashboard")
            }
        }
    }
}

@Composable
private fun LegendRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LegendChip(label = "Good health", color = Color(0xFF4CAF50), icon = Icons.Rounded.ChevronRight)
        LegendChip(label = "Blocked / use VPN", color = Color(0xFFFFC107), icon = Icons.Rounded.VpnLock)
        LegendChip(label = "Not good health", color = Color(0xFFEF5350), icon = Icons.Rounded.NorthEast)
    }
}

@Composable
private fun LegendChip(
    label: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = color, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun HealthReportRow(
    item: HealthReportItem,
) {
    val statusColor = when (item.status) {
        HealthStatus.OK -> Color(0xFF4CAF50)
        HealthStatus.BLOCKED,
        HealthStatus.LOGIN_REQUIRED,
        HealthStatus.REDIRECTED,
        HealthStatus.DNS_FAILED,
        HealthStatus.SSL_ISSUE,
        -> Color(0xFFFFC107)
        HealthStatus.DEAD,
        HealthStatus.TIMEOUT,
        -> Color(0xFFEF5350)
        HealthStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.title, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.normalizedUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            item.detailMessage?.let { detail ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = when (item.status) {
                HealthStatus.OK -> "Good"
                HealthStatus.BLOCKED -> "Blocked"
                HealthStatus.LOGIN_REQUIRED -> "Login"
                HealthStatus.REDIRECTED -> "Redirected"
                HealthStatus.DNS_FAILED -> "DNS failed"
                HealthStatus.SSL_ISSUE -> "TLS issue"
                HealthStatus.DEAD -> "Dead"
                HealthStatus.TIMEOUT -> "Timeout"
                HealthStatus.UNKNOWN -> "Unknown"
            },
            color = statusColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
