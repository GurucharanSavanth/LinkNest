package com.linknest.core.data.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.linknest.core.common.coroutine.IoDispatcher
import com.linknest.core.data.storage.LinkNestStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
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
    fun stageBackup(pkg: BackupPackage): File {
        val dir = LinkNestStorage.backupStagingDirectory(context).apply { mkdirs() }
        val ext = if (pkg.isEncrypted) "lnen" else "json"
        val file = File(dir, "latest-backup.$ext")
        // Remove any old staging files with different extension
        dir.listFiles()?.forEach { f ->
            if (f.name.startsWith("latest-backup.") && f.name != file.name) {
                f.delete()
            }
        }
        file.writeText(pkg.payload, Charsets.UTF_8)
        return file
    }

    fun getStagedBackup(): File? =
        LinkNestStorage.backupStagingDirectory(context)
            .listFiles()
            ?.filter { f -> f.name.startsWith("latest-backup.") }
            ?.maxByOrNull { f -> f.lastModified() }
            ?.takeIf { f -> f.exists() && f.length() > 0 }

    fun getStagedBackupInfo(): StagedBackupInfo? {
        val file = getStagedBackup() ?: return null
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
        val ext = file.extension.uppercase()
        val sizeKb = file.length() / 1024
        return StagedBackupInfo(
            file = file,
            dateLabel = date,
            typeLabel = if (ext == "LNEN") "Encrypted" else "JSON",
            sizeLabel = "${sizeKb}KB",
        )
    }

    suspend fun readStagedBackup(): ReadResult = withContext(ioDispatcher) {
        val file = getStagedBackup()
            ?: return@withContext ReadResult.Error("No backup found. Export first.")
        runCatching {
            val text = file.readText(Charsets.UTF_8)
            if (text.isBlank()) ReadResult.Error("Staged backup file is empty.")
            else ReadResult.Ok(text)
        }.getOrElse { e -> ReadResult.Error("Failed to read backup: ${e.message}") }
    }

    suspend fun writeToUri(
        resolver: ContentResolver,
        uri: Uri,
        pkg: BackupPackage,
    ): WriteResult = withContext(ioDispatcher) {
        runCatching {
            resolver.openOutputStream(uri)?.use { out ->
                out.write(pkg.payload.toByteArray(Charsets.UTF_8))
            } ?: return@withContext WriteResult.Error("Could not open file for writing.")
            WriteResult.Ok(uri.toString())
        }.getOrElse { e -> WriteResult.Error("Write failed: ${e.message}") }
    }

    suspend fun writeStagedToUri(
        resolver: ContentResolver,
        uri: Uri,
    ): WriteResult = withContext(ioDispatcher) {
        val file = getStagedBackup()
            ?: return@withContext WriteResult.Error("No staged backup to save.")
        runCatching {
            val bytes = file.readBytes()
            resolver.openOutputStream(uri)?.use { out -> out.write(bytes) }
                ?: return@withContext WriteResult.Error("Could not open file for writing.")
            WriteResult.Ok(uri.toString())
        }.getOrElse { e -> WriteResult.Error("Write failed: ${e.message}") }
    }

    suspend fun readFromUri(
        resolver: ContentResolver,
        uri: Uri,
    ): ReadResult = withContext(ioDispatcher) {
        runCatching {
            val text = resolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
            when {
                text == null -> ReadResult.Error("Could not open selected file.")
                text.isBlank() -> ReadResult.Error("Selected file is empty.")
                else -> ReadResult.Ok(text)
            }
        }.getOrElse { e -> ReadResult.Error("Failed to read file: ${e.message}") }
    }

    fun clearStaged() {
        runCatching {
            LinkNestStorage.backupStagingDirectory(context)
                .listFiles()
                ?.filter { it.name.startsWith("latest-backup.") }
                ?.forEach { it.delete() }
        }
    }
}

data class StagedBackupInfo(
    val file: File,
    val dateLabel: String,
    val typeLabel: String,
    val sizeLabel: String,
)
