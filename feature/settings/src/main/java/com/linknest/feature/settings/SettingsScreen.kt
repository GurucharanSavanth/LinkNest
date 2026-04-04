package com.linknest.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linknest.core.designsystem.component.GlassPanel
import com.linknest.core.designsystem.component.LinkNestGradientBackground
import com.linknest.core.model.HealthStatus
import com.linknest.core.model.TileDensityMode

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenIntegrityCenter: () -> Unit,
    onOpenHealthReport: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onMessageConsumed()
        }
    }

    SettingsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        onBack = onBack,
        onOpenIntegrityCenter = onOpenIntegrityCenter,
        onOpenHealthReport = onOpenHealthReport,
        onTileDensityModeSelected = viewModel::onTileDensityModeSelected,
        onTileSizeSelected = viewModel::onTileSizeSelected,
        onBackgroundHealthChecksChanged = viewModel::onBackgroundHealthChecksChanged,
        onEncryptedBackupsChanged = viewModel::onEncryptedBackupsChanged,
        onExportBackup = viewModel::onExportBackup,
        onImportPayloadChanged = viewModel::onImportPayloadChanged,
        onImportBackup = viewModel::onImportBackup,
        onRunHealthCheck = viewModel::onRunHealthCheck,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SettingsScreen(
    uiState: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onOpenIntegrityCenter: () -> Unit,
    onOpenHealthReport: () -> Unit,
    onTileDensityModeSelected: (TileDensityMode) -> Unit,
    onTileSizeSelected: (Int) -> Unit,
    onBackgroundHealthChecksChanged: (Boolean) -> Unit,
    onEncryptedBackupsChanged: (Boolean) -> Unit,
    onExportBackup: () -> Unit,
    onImportPayloadChanged: (String) -> Unit,
    onImportBackup: () -> Unit,
    onRunHealthCheck: () -> Unit,
) {
    LinkNestGradientBackground(modifier = modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
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
                            text = "Tools",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = onOpenIntegrityCenter) {
                                Text("Integrity Center")
                            }
                            OutlinedButton(onClick = onOpenHealthReport) {
                                Text("Health Report")
                            }
                        }
                    }
                }

                item {
                    GlassPanel {
                        Text(
                            text = "Tile Density",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            TileDensityMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = uiState.preferences.tileDensityMode == mode,
                                    onClick = { onTileDensityModeSelected(mode) },
                                    label = { Text(mode.name.lowercase().replaceFirstChar(Char::uppercaseChar)) },
                                )
                            }
                        }
                    }
                }

                item {
                    GlassPanel {
                        Text(
                            text = "Adaptive Tile Baseline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            listOf(132, 148, 164, 180, 196).forEach { tileSize ->
                                FilterChip(
                                    selected = uiState.preferences.tileSizeDp == tileSize,
                                    onClick = { onTileSizeSelected(tileSize) },
                                    label = { Text("${tileSize}dp") },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Dashboard still adapts to content, icon source, and screen width. This sets the minimum density target.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item {
                    GlassPanel {
                        SettingToggleRow(
                            title = "Background health monitoring",
                            description = "Allow periodic health checks to run in the background with network constraints.",
                            checked = uiState.preferences.backgroundHealthChecksEnabled,
                            icon = {
                                Icon(Icons.Rounded.HealthAndSafety, contentDescription = null)
                            },
                            onCheckedChange = onBackgroundHealthChecksChanged,
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        SettingToggleRow(
                            title = "Encrypted backup exports",
                            description = "Use Android Keystore-backed AES-GCM encryption for exported backup payloads.",
                            checked = uiState.preferences.encryptedBackupsEnabled,
                            icon = {
                                Icon(Icons.Rounded.Lock, contentDescription = null)
                            },
                            onCheckedChange = onEncryptedBackupsChanged,
                        )
                    }
                }

                item {
                    GlassPanel {
                    Text(
                        text = "Backup & Restore",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onExportBackup, enabled = !uiState.isExporting) {
                            if (uiState.isExporting) {
                                CircularProgressIndicator(strokeWidth = 2.dp)
                            } else {
                                Text(if (uiState.preferences.encryptedBackupsEnabled) "Export Backup" else "Export JSON")
                            }
                        }
                        OutlinedButton(onClick = onImportBackup, enabled = !uiState.isImporting) {
                            if (uiState.isImporting) {
                                CircularProgressIndicator(strokeWidth = 2.dp)
                            } else {
                                Text("Import JSON")
                            }
                        }
                    }
                    uiState.backupFilePath?.let { path ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Last export: $path",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = if (uiState.importPayload.isNotBlank()) uiState.importPayload else uiState.backupJson,
                        onValueChange = onImportPayloadChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Backup payload (.json or encrypted .lnen)") },
                        minLines = 5,
                    )
                }
                }

                item {
                    GlassPanel {
                    Text(
                        text = "Health Monitor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onRunHealthCheck,
                        enabled = !uiState.isRunningHealthCheck,
                    ) {
                        if (uiState.isRunningHealthCheck) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                        } else {
                            Text("Run health check")
                        }
                    }
                    uiState.healthSummary?.let { summary ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (uiState.latestHealthReport.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Latest report",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.latestHealthReport.take(6).forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = item.title, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = item.normalizedUrl,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
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
                                        color = when (item.status) {
                                            HealthStatus.OK -> Color(0xFF4CAF50)
                                            HealthStatus.BLOCKED,
                                            HealthStatus.LOGIN_REQUIRED,
                                            HealthStatus.REDIRECTED,
                                            HealthStatus.DNS_FAILED,
                                            HealthStatus.SSL_ISSUE,
                                            -> Color(0xFFFFC107)
                                            HealthStatus.DEAD,
                                            HealthStatus.TIMEOUT,
                                            -> MaterialTheme.colorScheme.error
                                            HealthStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        style = MaterialTheme.typography.labelMedium,
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
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    icon: @Composable () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon()
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
