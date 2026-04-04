package com.linknest.feature.dashboard

import android.graphics.Color.parseColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.linknest.core.designsystem.component.GlassPanel
import com.linknest.core.model.DashboardCategory
import com.linknest.core.model.DashboardSmartSection
import com.linknest.core.model.HealthStatus
import com.linknest.core.model.WebsiteListItem

private val CategoryActionColors = listOf("#7C4DFF", "#00BCD4", "#4CAF50", "#FF9800", "#F06292", "#FFD54F")
private val CategoryActionIcons = listOf("🧠", "📁", "🚀", "🎨", "💼", "🌐")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSectionSheet(
    section: DashboardSmartSection,
    onDismiss: () -> Unit,
    onOpenWebsite: (WebsiteListItem) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(section.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            section.websites.forEach { website ->
                val metadataSummary = website.dashboardMetadataSummary()
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            onDismiss()
                            onOpenWebsite(website)
                        },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(website.title, fontWeight = FontWeight.Medium)
                            Text(website.normalizedUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            metadataSummary?.let { summary ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text(website.openCount.toString(), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebsiteActionsSheet(
    website: WebsiteListItem,
    categories: List<DashboardCategory>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onReorderInCategory: () -> Unit,
    onRelocate: (Long) -> Unit,
    onPinToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(website.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            SheetActionButton("Edit website", Icons.Rounded.Edit, onEdit)
            SheetActionButton("Reorder inside category", Icons.Rounded.DragIndicator, onReorderInCategory)
            SheetActionButton(if (website.isPinned) "Unpin website" else "Pin website", Icons.Rounded.PushPin, onPinToggle)
            Text("Move to another category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            categories.filterNot { it.id == website.categoryId }.forEach { category ->
                AssistChip(
                    onClick = { onRelocate(category.id) },
                    label = { Text(category.name) },
                    leadingIcon = { Icon(Icons.Rounded.Workspaces, contentDescription = null) },
                )
            }
            SheetActionButton("Delete website", Icons.Rounded.Delete, onDelete, destructive = true)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryActionsSheet(
    category: DashboardCategory,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onReorder: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(category.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            SheetActionButton("Rename or recolor", Icons.Rounded.Edit, onEdit)
            SheetActionButton("Reorder category", Icons.Rounded.DragIndicator, onReorder)
            SheetActionButton("Archive category", Icons.Rounded.Archive, onArchive)
            SheetActionButton("Delete category", Icons.Rounded.Delete, onDelete, destructive = true)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryReorderSheet(
    categories: List<DashboardCategory>,
    selectedCategoryId: Long,
    onDismiss: () -> Unit,
    onMoveToIndex: (Int) -> Unit,
) {
    val selected = categories.firstOrNull { it.id == selectedCategoryId } ?: return
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Place ${selected.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            categories.forEachIndexed { index, category ->
                TextButton(
                    onClick = { onMoveToIndex(index) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = category.id != selectedCategoryId,
                ) {
                    Text("Position ${index + 1}: ${category.name}")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebsiteReorderSheet(
    category: DashboardCategory,
    selectedWebsiteId: Long,
    onDismiss: () -> Unit,
    onMoveToIndex: (Int) -> Unit,
) {
    val selected = category.websites.firstOrNull { it.id == selectedWebsiteId } ?: return
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Place ${selected.title}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(category.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            category.websites.forEachIndexed { index, website ->
                TextButton(
                    onClick = { onMoveToIndex(index) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = website.id != selectedWebsiteId,
                ) {
                    Text("Position ${index + 1}: ${website.title}")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SheetActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(0.dp))
        Text(text, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditCategorySheet(
    category: DashboardCategory,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String?) -> Unit,
) {
    var name by remember(category.id) { mutableStateOf(category.name) }
    var colorHex by remember(category.id) { mutableStateOf(category.colorHex) }
    var iconValue by remember(category.id) { mutableStateOf(category.iconValue ?: CategoryActionIcons.first()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Edit Category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Category name") }, singleLine = true)
            Text("Color", fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CategoryActionColors.forEach { candidate ->
                    Surface(
                        modifier = Modifier.size(36.dp).clip(CircleShape).clickable { colorHex = candidate },
                        color = Color(parseColor(candidate)),
                        tonalElevation = if (candidate == colorHex) 4.dp else 0.dp,
                    ) {}
                }
            }
            Text("Icon", fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CategoryActionIcons.forEach { candidate ->
                    AssistChip(onClick = { iconValue = candidate }, label = { Text(candidate) })
                }
            }
            Button(onClick = { onSubmit(name.trim(), colorHex, iconValue) }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text("Save category")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { Button(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

fun healthStatusColor(status: HealthStatus): Color = when (status) {
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
    HealthStatus.UNKNOWN -> Color(0xFF90A4AE)
}
