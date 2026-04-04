package com.linknest.app.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.linknest.app.MainActivity
import com.linknest.core.action.ActionResult
import com.linknest.core.action.model.ShareCapturePipelineInput
import com.linknest.core.action.pipeline.ShareCapturePipeline
import com.linknest.core.designsystem.component.GlassPanel
import com.linknest.core.designsystem.component.LinkNestGradientBackground
import com.linknest.core.designsystem.theme.LinkNestTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShareEntryActivity : ComponentActivity() {
    @Inject
    lateinit var shareCapturePipeline: ShareCapturePipeline

    private var uiState by mutableStateOf<ShareCaptureUiState>(ShareCaptureUiState.Loading)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LinkNestTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ShareEntryScreen(
                        uiState = uiState,
                        onOpenDashboard = ::openDashboard,
                        onClose = ::finish,
                    )
                }
            }
        }
        handleSharedIntent(intent)
    }

    private fun handleSharedIntent(intent: Intent?) {
        val sharedText = extractSharedText(intent)
        if (sharedText.isBlank()) {
            uiState = ShareCaptureUiState.Error(
                message = "No usable website URL was found in the shared content.",
            )
            return
        }

        lifecycleScope.launch {
            when (
                val result = shareCapturePipeline(
                    ShareCapturePipelineInput(sharedText = sharedText),
                )
            ) {
                is ActionResult.Success -> openAddWebsite(result.value.normalizedUrl)
                is ActionResult.PartialSuccess -> openAddWebsite(result.value.normalizedUrl)
                is ActionResult.Failure -> {
                    uiState = ShareCaptureUiState.Error(message = result.issue.message)
                }
            }
        }
    }

    private fun extractSharedText(intent: Intent?): String {
        if (intent?.action != Intent.ACTION_SEND) return ""
        val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        if (extraText.isNotBlank()) return extraText
        return intent.clipData
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(this)
            ?.toString()
            ?.trim()
            .orEmpty()
    }

    private fun openAddWebsite(normalizedUrl: String) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("linknest://add?url=${Uri.encode(normalizedUrl)}"),
                this,
                MainActivity::class.java,
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                `package` = packageName
            },
        )
        finish()
    }

    private fun openDashboard() {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("linknest://dashboard"),
                this,
                MainActivity::class.java,
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                `package` = packageName
            },
        )
        finish()
    }
}

private sealed interface ShareCaptureUiState {
    data object Loading : ShareCaptureUiState

    data class Error(
        val message: String,
    ) : ShareCaptureUiState
}

@Composable
private fun ShareEntryScreen(
    uiState: ShareCaptureUiState,
    onOpenDashboard: () -> Unit,
    onClose: () -> Unit,
) {
    LinkNestGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                when (uiState) {
                    ShareCaptureUiState.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Capturing shared link",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "LinkNest is validating the shared content and preparing the Add Website flow.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    is ShareCaptureUiState.Error -> {
                        Text(
                            text = "Unable to capture this share",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onOpenDashboard,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Open LinkNest")
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onClose,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}
