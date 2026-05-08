package com.linknest.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    val backupFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        viewModel.onBackupFolderSelected(uri?.toString())
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) {
            viewModel.onExportSaveCancelled()
        } else {
            viewModel.onSaveExportToUri(context.contentResolver, uri)
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.onImportFromUri(context.contentResolver, uri)
        }
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onMessageConsumed()
        }
    }

    LaunchedEffect(uiState.exportStatus) {
        val status = uiState.exportStatus
        if (status is ExportStatus.ReadyToSave) {
            saveLauncher.launch(status.pkg.fileName)
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
        onBackgroundHealthChecksChanged = viewModel::onBackgroundHealthChecksChanged,
        onSetBackupFolder = { backupFolderLauncher.launch(null) },
        onClearBackupFolder = { viewModel.onBackupFolderSelected(null) },
        onExportBackup = viewModel::onExportBackup,
        onImportFromStaged = viewModel::onImportFromStaged,
        onImportFromFile = { importFileLauncher.launch(arrayOf("*/*")) },
        onImportFromText = viewModel::onImportFromText,
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
    onBackgroundHealthChecksChanged: (Boolean) -> Unit,
    onSetBackupFolder: () -> Unit,
    onClearBackupFolder: () -> Unit,
    onExportBackup: () -> Unit,
    onImportFromStaged: () -> Unit,
    onImportFromFile: () -> Unit,
    onImportFromText: (String) -> Unit,
    onRunHealthCheck: () -> Unit,
) {
    var showPasteDialog by rememberSaveable { mutableStateOf(false) }

    if (showPasteDialog) {
        PasteImportDialog(
            onDismiss = { showPasteDialog = false },
            onImport = { text ->
                showPasteDialog = false
                onImportFromText(text)
            },
        )
    }

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
                        Text("Tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = onOpenIntegrityCenter) { Text("Integrity Center") }
                            OutlinedButton(onClick = onOpenHealthReport) { Text("Health Report") }
                        }
                    }
                }

                item {
                    GlassPanel {
                        Text("Tile Density", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            TileDensityMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = uiState.preferences.tileDensityMode == mode,
                                    onClick = { onTileDensityModeSelected(mode) },
                                    label = { Text(mode.name.lowercase().replaceFirstChar(Char::uppercaseChar)) },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Controls how many tiles fit per row in grid mode.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item {
                    GlassPanel {
                        SettingToggle(
                            title = "Background health monitoring",
                            description = "Periodic health checks in background with network constraints.",
                            checked = uiState.preferences.backgroundHealthChecksEnabled,
                            icon = { Icon(Icons.Rounded.HealthAndSafety, contentDescription = null) },
                            onCheckedChange = onBackgroundHealthChecksChanged,
                        )
                    }
                }

                item {
                    BackupRestorePanel(
                        uiState = uiState,
                        onSetBackupFolder = onSetBackupFolder,
                        onClearBackupFolder = onClearBackupFolder,
                        onExportBackup = onExportBackup,
                        onImportFromStaged = onImportFromStaged,
                        onImportFromFile = onImportFromFile,
                        onPasteImport = { showPasteDialog = true },
                    )
                }

                item {
                    GlassPanel {
                        Text("Health Monitor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = onRunHealthCheck, enabled = !uiState.isRunningHealthCheck) {
                            if (uiState.isRunningHealthCheck) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.padding(start = 8.dp))
                                Text("Checking...")
                            } else {
                                Text("Run health check")
                            }
                        }
                        uiState.healthSummary?.let { summary ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(summary, style = MaterialTheme.typography.bodySmall)
                        }
                        if (uiState.latestHealthReport.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Latest report", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                uiState.latestHealthReport.take(6).forEach { item ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.title, fontWeight = FontWeight.Medium)
                                            Text(item.normalizedUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(
                                            text = item.status.displayName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = when (item.status) {
                                                HealthStatus.OK -> Color(0xFF4CAF50)
                                                HealthStatus.DEAD, HealthStatus.TIMEOUT -> MaterialTheme.colorScheme.error
                                                HealthStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
                                                else -> Color(0xFFFFC107)
                                            },
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackupRestorePanel(
    uiState: SettingsUiState,
    onSetBackupFolder: () -> Unit,
    onClearBackupFolder: () -> Unit,
    onExportBackup: () -> Unit,
    onImportFromStaged: () -> Unit,
    onImportFromFile: () -> Unit,
    onPasteImport: () -> Unit,
) {
    GlassPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Backup & Restore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (uiState.exportStatus is ExportStatus.Saved) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Saved", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
            }
        }

        uiState.stagedInfo?.let { info ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Last export: ${info.dateLabel}  •  ${info.typeLabel}  •  ${info.sizeLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Export section
        Text("Export", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onExportBackup,
                enabled = !uiState.isExporting && !uiState.isImporting,
                modifier = Modifier.weight(1f),
            ) {
                if (uiState.isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.padding(start = 6.dp))
                    Text("Export Backup")
                }
            }
        }

        // Backup folder row
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                val folderUri = uiState.preferences.backupFolderUri
                Text(
                    text = if (folderUri != null) "Auto-save folder set" else "No auto-save folder",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (folderUri != null) FontWeight.SemiBold else FontWeight.Normal,
                )
                if (folderUri != null) {
                    Text(folderUri.takeLast(48), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(onClick = onSetBackupFolder) {
                Text(if (uiState.preferences.backupFolderUri != null) "Change" else "Set")
            }
            if (uiState.preferences.backupFolderUri != null) {
                TextButton(onClick = onClearBackupFolder) { Text("Clear") }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

        // Import section
        Text("Import", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))

        when (val status = uiState.importStatus) {
            is ImportStatus.Running -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Restoring data…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            is ImportStatus.Success -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                    Text(
                        "Restored ${status.websiteCount} websites in ${status.categoryCount} categories.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4CAF50),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            is ImportStatus.Failed -> {
                Text("Import failed: ${status.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }
            else -> Unit
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (uiState.hasStagedBackup) {
                Button(
                    onClick = onImportFromStaged,
                    enabled = !uiState.isImporting && !uiState.isExporting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.padding(start = 6.dp))
                    Text("Import last export${uiState.stagedInfo?.let { " (${it.dateLabel})" }.orEmpty()}")
                }
            }
            OutlinedButton(
                onClick = onImportFromFile,
                enabled = !uiState.isImporting && !uiState.isExporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Import from file")
            }
            OutlinedButton(
                onClick = onPasteImport,
                enabled = !uiState.isImporting && !uiState.isExporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Paste backup text")
            }
        }

    }
}

@Composable
private fun PasteImportDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paste backup") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Backup payload (.json or .lnen)") },
                minLines = 5,
                maxLines = 12,
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onImport(text) }, enabled = text.isNotBlank()) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    icon: @Composable () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        icon()
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
