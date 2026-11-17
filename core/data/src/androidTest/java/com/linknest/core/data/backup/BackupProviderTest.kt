package com.linknest.core.data.backup

import android.content.ContentResolver
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class BackupProviderTest {
    @Test fun providerReadsHandleNullRevocationShortChunksAndTruncation() = fixture { manager, resolver, provider ->
        runBlocking {
            provider.failure = Failure.NULL_STREAM
            assertTrue(manager.readFromUri(resolver, provider.existingUri) is ReadResult.Error)
            provider.failure = Failure.REVOKED
            assertTrue(manager.readFromUri(resolver, provider.existingUri) is ReadResult.Error)
            provider.failure = Failure.NONE
            provider.input = "small provider stream".toByteArray()
            assertEquals("small provider stream", (manager.readFromUri(resolver, provider.existingUri) as ReadResult.Ok).payload)
            provider.input = "truncated JSON".toByteArray()
            val truncated = manager.readFromUri(resolver, provider.existingUri) as ReadResult.Ok
            assertThrows(org.json.JSONException::class.java) { BackupSerializer().deserialize(truncated.payload) }
        }
    }

    @Test fun failedCreatedDocumentsAreCleanedWithoutDeletingExistingDocuments() = fixture { manager, resolver, provider ->
        runBlocking {
            val pkg = BackupPackage("payload", "test.json", "unused")
            for (failure in listOf(Failure.NULL_STREAM, Failure.OPEN_FAILED, Failure.BROKEN_PIPE)) {
                provider.failure = failure
                val existing = manager.writeToUri(resolver, provider.existingUri, pkg)
                assertTrue("Existing write must fail for $failure", existing is WriteResult.Error)
                assertTrue(provider.deleted.none { it == "existing" })
                val createdCount = provider.created.size
                val deletedCount = provider.deleted.size
                val created = manager.writeToFolder(resolver, provider.treeUri, pkg)
                assertTrue("Folder write must fail for $failure", created is WriteResult.Error)
                assertEquals(createdCount + 1, provider.created.size)
                assertEquals(deletedCount + 1, provider.deleted.size)
                assertEquals(provider.created.last(), provider.deleted.last())
            }
            provider.failure = Failure.CREATE_FAILED
            val count = provider.deleted.size
            assertTrue(manager.writeToFolder(resolver, provider.treeUri, pkg) is WriteResult.Error)
            assertEquals(count, provider.deleted.size)
        }
    }

    @Test fun successfulProviderWriteIsClosedAndContainsExactPayload() = fixture { manager, resolver, provider ->
        runBlocking {
            val pkg = BackupPackage("UTF-8 保存", "test.json", "unused")
            assertTrue(manager.writeToFolder(resolver, provider.treeUri, pkg) is WriteResult.Ok)
            assertEquals(pkg.payload, File(provider.directory, provider.created.single()).readText())
            assertTrue(provider.deleted.isEmpty())
        }
    }

    private fun fixture(test: (BackupFileManager, ContentResolver, Provider) -> Unit) {
        assumeTrue("ContentResolver.wrap requires API 29", android.os.Build.VERSION.SDK_INT >= 29)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "provider-test-${UUID.randomUUID()}").apply { check(mkdir()) }
        val provider = Provider(directory)
        provider.attachInfo(context, ProviderInfo().apply {
            authority = AUTHORITY; exported = true; grantUriPermissions = true
            readPermission = "android.permission.MANAGE_DOCUMENTS"; writePermission = readPermission
        })
        try { test(BackupFileManager(context, Dispatchers.IO), ContentResolver.wrap(provider), provider) }
        finally {
            provider.writers.forEach { it.join(3000); check(!it.isAlive) }
            provider.shutdown()
            check(directory.deleteRecursively())
        }
    }

    private enum class Failure { NONE, NULL_STREAM, REVOKED, CREATE_FAILED, OPEN_FAILED, BROKEN_PIPE }

    private class Provider(val directory: File) : DocumentsProvider() {
        var failure = Failure.NONE
        var input = ByteArray(0)
        val created = mutableListOf<String>()
        val deleted = mutableListOf<String>()
        val writers = mutableListOf<Thread>()
        val existingUri: Uri = DocumentsContract.buildDocumentUri(AUTHORITY, "existing")
        val treeUri: Uri = DocumentsContract.buildTreeDocumentUri(AUTHORITY, "root")
        override fun onCreate() = true
        override fun queryRoots(projection: Array<out String>?): Cursor = MatrixCursor(arrayOf(DocumentsContract.Root.COLUMN_ROOT_ID))
        override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor = MatrixCursor(arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_FLAGS,
        )).apply { addRow(arrayOf(documentId, "application/json", documentId, 0)) }
        override fun queryChildDocuments(parentDocumentId: String, projection: Array<out String>?, sortOrder: String?): Cursor =
            MatrixCursor(arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
        override fun isChildDocument(parentDocumentId: String, documentId: String) = true
        override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {
            if (failure == Failure.CREATE_FAILED) throw IOException("injected creation failure")
            return "created-${created.size}".also { created += it }
        }
        override fun deleteDocument(documentId: String) { deleted += documentId; File(directory, documentId).delete() }
        override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor? {
            when (failure) {
                Failure.NULL_STREAM -> return null
                Failure.REVOKED -> throw SecurityException("injected revoked permission")
                Failure.OPEN_FAILED -> throw IOException("injected provider failure")
                Failure.BROKEN_PIPE -> {
                    val pipe = ParcelFileDescriptor.createReliablePipe()
                    pipe[0].closeWithError("injected ENOSPC")
                    return pipe[1]
                }
                else -> Unit
            }
            if (mode == "r") {
                val pipe = ParcelFileDescriptor.createPipe()
                writers += thread(isDaemon = true, name = "backup-provider-fixture") {
                    ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { output -> input.forEach { output.write(it.toInt()) } }
                }
                return pipe[0]
            }
            return ParcelFileDescriptor.open(File(directory, documentId), ParcelFileDescriptor.MODE_CREATE or
                ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_TRUNCATE)
        }
    }

    private companion object { const val AUTHORITY = "com.linknest.backup.fixture" }
}
