package com.linknest.app

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.filters.SdkSuppress
import com.linknest.feature.settings.ExportStatus
import com.linknest.feature.settings.ImportStatus
import com.linknest.feature.settings.SettingsViewModel
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@SdkSuppress(minSdkVersion = 29)
class SettingsOperationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun rapidImportRequests_openOnlyOneStream_andCannotResetRunningOperation() {
        val provider = GatedBackupProvider()
        provider.attachInfo(compose.activity, ProviderInfo().apply { authority = "com.linknest.test.invalid-backup" })
        val resolver = ContentResolver.wrap(provider)
        lateinit var viewModel: SettingsViewModel
        try {
            compose.runOnIdle {
                viewModel = ViewModelProvider(compose.activity)[SettingsViewModel::class.java]
                viewModel.onImportFromUri(resolver, Uri.parse("content://com.linknest.test.invalid-backup/input"))
                assertTrue(viewModel.uiState.value.importStatus is ImportStatus.Running)
                viewModel.resetImportStatus()
                assertTrue(viewModel.uiState.value.importStatus is ImportStatus.Running)
                viewModel.onImportFromUri(resolver, Uri.parse("content://com.linknest.test.invalid-backup/input"))
                viewModel.onExportBackup()
            }
            assertTrue(provider.opened.await(5, TimeUnit.SECONDS))
        } finally {
            provider.release.countDown()
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.importStatus is ImportStatus.Failed
        }
        assertEquals(1, provider.openCount.get())
        assertEquals(ExportStatus.Idle, viewModel.uiState.value.exportStatus)
    }

    @Test fun repeatedExportCallback_keepsBackupBusyUntilFirstWriteFinishes() {
        val output = File.createTempFile("settings-export-", ".json", compose.activity.cacheDir)
        val provider = GatedBackupProvider(output)
        val authority = "com.linknest.test.export-backup"
        provider.attachInfo(compose.activity, ProviderInfo().apply { this.authority = authority })
        val resolver = ContentResolver.wrap(provider)
        val uri = Uri.parse("content://$authority/output")
        try {
            val viewModel = compose.runOnIdle { ViewModelProvider(compose.activity)[SettingsViewModel::class.java] }
            compose.waitUntil(timeoutMillis = 10_000) { !viewModel.uiState.value.isLoading }
            compose.runOnIdle {
                check(viewModel.uiState.value.preferences.backupFolderUri == null) { "Use a disposable install without an auto-save folder" }
                viewModel.onExportBackup()
            }
            compose.waitUntil(timeoutMillis = 10_000) { viewModel.uiState.value.exportStatus is ExportStatus.ReadyToSave }
            val payload = (viewModel.uiState.value.exportStatus as ExportStatus.ReadyToSave).pkg.payload
            try {
                compose.runOnIdle { viewModel.onSaveExportToUri(resolver, uri) }
                assertTrue(provider.opened.await(5, TimeUnit.SECONDS))
                compose.runOnIdle {
                    viewModel.onSaveExportToUri(resolver, uri)
                    assertEquals(ExportStatus.Building, viewModel.uiState.value.exportStatus)
                    assertTrue(viewModel.uiState.value.isBackupBusy)
                }
            } finally {
                provider.release.countDown()
            }
            compose.waitUntil(timeoutMillis = 10_000) { !viewModel.uiState.value.isBackupBusy }
            assertEquals(ExportStatus.Saved(uri.toString()), viewModel.uiState.value.exportStatus)
            assertEquals(1, provider.openCount.get())
            assertEquals(payload, output.readText())
        } finally {
            output.delete()
        }
    }

    private class GatedBackupProvider(private val output: File? = null) : ContentProvider() {
        val opened = CountDownLatch(1)
        val release = CountDownLatch(1)
        val openCount = AtomicInteger()
        override fun onCreate() = true
        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
            openCount.incrementAndGet()
            opened.countDown()
            check(release.await(5, TimeUnit.SECONDS))
            if (output != null) {
                return ParcelFileDescriptor.open(output, ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_TRUNCATE)
            }
            // Invalid JSON fails before preview, recovery staging, or database replacement.
            val pipe = ParcelFileDescriptor.createPipe()
            ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { it.write("{}".toByteArray()) }
            return pipe[0]
        }
        override fun getType(uri: Uri) = "application/json"
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    }
}
