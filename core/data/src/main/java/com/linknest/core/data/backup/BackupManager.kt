package com.linknest.core.data.backup

import com.linknest.core.common.coroutine.IoDispatcher
import com.linknest.core.data.model.BackupArtifact
import com.linknest.core.data.model.BackupSnapshot
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class BackupManager @Inject constructor(
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val serializer: BackupSerializer,
    private val fileManager: BackupFileManager,
) {
    suspend fun export(snapshot: BackupSnapshot, encrypted: Boolean): BackupArtifact =
        withContext(ioDispatcher) {
            val pkg = serializer.serialize(snapshot, encrypted)
            val staged = runCatching { fileManager.stageBackup(pkg) }.getOrNull()
            BackupArtifact(
                fileName = pkg.fileName,
                filePath = staged?.absolutePath,
                json = pkg.payload,
                isEncrypted = pkg.isEncrypted,
                checksum = pkg.checksum,
            )
        }

    fun parse(payload: String): BackupSnapshot = serializer.deserialize(payload)
}
