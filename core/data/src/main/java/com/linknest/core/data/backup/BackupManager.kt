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
    suspend fun export(snapshot: BackupSnapshot, encrypted: Boolean = false): BackupArtifact =
        withContext(ioDispatcher) {
            val pkg = serializer.serialize(snapshot)
            val staged = runCatching { fileManager.stageBackup(pkg) }.getOrNull()
            BackupArtifact(
                fileName = pkg.fileName,
                filePath = staged?.absolutePath,
                json = pkg.payload,
                isEncrypted = false,
                checksum = pkg.checksum,
            )
        }

    fun parse(payload: String): BackupSnapshot = serializer.deserialize(payload)
}
