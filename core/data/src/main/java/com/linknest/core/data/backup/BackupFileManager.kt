package com.linknest.core.data.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.linknest.core.common.coroutine.IoDispatcher
import com.linknest.core.data.storage.LinkNestStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

sealed interface ReadResult {
    data class Ok(val payload: String) : ReadResult
    data class Error(val message: String) : ReadResult
}

sealed interface WriteResult {
    data class Ok(val uriLabel: String) : WriteResult
    data class Error(val message: String) : WriteResult
}

@Singleton
class BackupFileManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val recoveryStore get() = RecoveryBackupStore(
        LinkNestStorage.backupStagingDirectory(context), BackupSerializer(),
    )

    @Synchronized fun stageRecovery(pkg: BackupPackage): File = recoveryStore.stage(pkg)

    @Synchronized fun completeRecovery(file: File) = recoveryStore.complete(file)

    @Synchronized fun prepareRecoveryOperation(name: String, current: com.linknest.core.data.model.BackupSnapshot, incoming: com.linknest.core.data.model.BackupSnapshot) =
        recoveryStore.prepare(name, current, incoming)

    @Synchronized fun markRecoveryCommitted(name: String) = recoveryStore.committed(name)

    @Synchronized fun reconcileRecovery(current: com.linknest.core.data.model.BackupSnapshot): List<com.linknest.core.data.model.RecoveryReconciliation> =
        recoveryStore.reconcile(current)

    @Synchronized fun getRecoveryBackupInfo(): StagedBackupInfo? =
        runCatching { recoveryStore.latest()?.let { info(it, "Recovery JSON") } }.getOrNull()

    suspend fun readRecoveryBackup(): ReadResult = withContext(ioDispatcher) {
        readResult("Failed to read recovery") {
            synchronized(this@BackupFileManager) {
                val file = recoveryStore.latest()
                    ?: return@readResult ReadResult.Error("No verified recovery snapshot is available.")
                ReadResult.Ok(file.bufferedReader(Charsets.UTF_8).use { it.readBackupText() })
            }
        }
    }

    @Synchronized fun stageBackup(pkg: BackupPackage): File {
        val dir = LinkNestStorage.backupStagingDirectory(context).apply { mkdirs() }
        val file = File(dir, "latest-backup.json")
        val atomic = android.util.AtomicFile(file)
        val output = atomic.startWrite()
        try {
            output.write(pkg.payload.toByteArray(Charsets.UTF_8))
            output.flush()
            output.fd.sync()
            atomic.finishWrite(output)
        } catch (failure: Exception) {
            atomic.failWrite(output)
            throw failure
        }
        // AtomicFile can log a failed rename without throwing; verify the published base file.
        val published = atomic.openRead().bufferedReader(Charsets.UTF_8).use { it.readBackupText() }
        if (published != pkg.payload) throw java.io.IOException("Staged backup publication could not be verified.")
        return file
    }

    @Synchronized fun getStagedBackup(): File? {
        val file = File(LinkNestStorage.backupStagingDirectory(context), "latest-backup.json")
        return try {
            android.util.AtomicFile(file).openRead().use { }
            file.takeIf { it.length() > 0 }
        } catch (_: java.io.FileNotFoundException) { null }
    }

    fun getStagedBackupInfo(): StagedBackupInfo? = getStagedBackup()?.let { info(it, "Export JSON") }

    private fun info(file: File, type: String): StagedBackupInfo {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
        return StagedBackupInfo(file, date, type, "${file.length() / 1024}KB")
    }

    suspend fun readStagedBackup(): ReadResult = withContext(ioDispatcher) {
        val file = getStagedBackup()
            ?: return@withContext ReadResult.Error("No backup found. Export first.")
        readResult("Failed to read backup") {
            val text = file.bufferedReader(Charsets.UTF_8).use { it.readBackupText() }
            if (text.isBlank()) ReadResult.Error("Staged backup file is empty.")
            else ReadResult.Ok(text)
        }
    }

    suspend fun writeToUri(
        resolver: ContentResolver,
        uri: Uri,
        pkg: BackupPackage,
    ): WriteResult = withContext(ioDispatcher) {
        writeResult("Write failed") {
            writeBackupPayload(resolver.openOutputStream(uri), pkg.payload)
            WriteResult.Ok(uri.toString())
        }
    }

    /**
     * Auto-saves [pkg] into a previously-granted SAF tree ([treeUri] from OpenDocumentTree),
     * creating a new document there. Used when the user has set a backup folder so exports save
     * without re-picking a location. Returns Error (caller falls back to the manual picker) if the
     * grant was revoked or the document can't be created.
     */
    suspend fun writeToFolder(
        resolver: ContentResolver,
        treeUri: Uri,
        pkg: BackupPackage,
    ): WriteResult = withContext(ioDispatcher) {
        var created: Uri? = null
        writeResult("Auto-save failed") {
            try {
                val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri),
                )
                val target = DocumentsContract.createDocument(
                    resolver,
                    parentDocUri,
                    "application/json",
                    pkg.fileName,
                ) ?: return@withContext WriteResult.Error("Could not create the backup file in the chosen folder.")
                created = target
                writeBackupPayload(resolver.openOutputStream(target), pkg.payload)
                WriteResult.Ok(target.toString())
            } catch (failure: Exception) {
                created?.let { runCatching { DocumentsContract.deleteDocument(resolver, it) } }
                throw failure
            }
        }
    }

    suspend fun writeStagedToUri(
        resolver: ContentResolver,
        uri: Uri,
    ): WriteResult = withContext(ioDispatcher) {
        val file = getStagedBackup()
            ?: return@withContext WriteResult.Error("No staged backup to save.")
        writeResult("Write failed") {
            val payload = file.bufferedReader(Charsets.UTF_8).use { it.readBackupText() }
            writeBackupPayload(resolver.openOutputStream(uri), payload)
            WriteResult.Ok(uri.toString())
        }
    }

    suspend fun readFromUri(
        resolver: ContentResolver,
        uri: Uri,
    ): ReadResult = withContext(ioDispatcher) {
        readResult("Failed to read file") {
            val text = resolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readBackupText() }
            when {
                text == null -> ReadResult.Error("Could not open selected file.")
                text.isBlank() -> ReadResult.Error("Selected file is empty.")
                else -> ReadResult.Ok(text)
            }
        }
    }

    @Synchronized fun clearStaged() {
        android.util.AtomicFile(File(LinkNestStorage.backupStagingDirectory(context), "latest-backup.json")).delete()
    }

    private suspend inline fun readResult(label: String, block: () -> ReadResult): ReadResult {
        currentCoroutineContext().ensureActive()
        return try { block() } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { ReadResult.Error("$label: ${failure.message}") }
    }

    private suspend inline fun writeResult(label: String, block: () -> WriteResult): WriteResult {
        currentCoroutineContext().ensureActive()
        return try { block() } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { WriteResult.Error("$label: ${failure.message}") }
    }
}

data class StagedBackupInfo(
    val file: File,
    val dateLabel: String,
    val typeLabel: String,
    val sizeLabel: String,
)
