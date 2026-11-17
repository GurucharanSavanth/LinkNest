package com.linknest.core.data.backup

import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.linknest.core.data.model.BackupSnapshot
import com.linknest.core.data.storage.LinkNestStorage
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RecoveryBackupStoreTest {
    private val serializer = BackupSerializer()
    private fun pkg(time: Long) = serializer.serialize(BackupSnapshot(
        schemaVersion = 6, exportedAt = time, categories = listOf(com.linknest.core.data.model.BackupCategory(
            1, "Snapshot $time", "#000000", com.linknest.core.model.IconType.AUTO, null, 0, false, false, 1, 1)), websites = emptyList(),
        tags = emptyList(), websiteTags = emptyList(), mappings = emptyList(),
        preferences = com.linknest.core.data.model.BackupPreferences("LIST", 80 + time.toInt(), "ADAPTIVE", true, false),
    ))

    @Test fun exportsCannotReplaceRecoveryAndCorruptNewestFallsBackAfterRestart() = fixture { context ->
        runBlocking {
            val manager = BackupFileManager(context, Dispatchers.IO)
            val first = manager.stageRecovery(pkg(1))
            manager.completeRecovery(first)
            val second = manager.stageRecovery(pkg(2))
            manager.completeRecovery(second)
            manager.stageBackup(pkg(3))
            manager.stageBackup(pkg(4))
            assertEquals(pkg(2).payload, (manager.readRecoveryBackup() as ReadResult.Ok).payload)
            manager.clearStaged()
            second.writeText("corrupt")
            val restarted = BackupFileManager(context, Dispatchers.IO)
            assertEquals(pkg(1).payload, (restarted.readRecoveryBackup() as ReadResult.Ok).payload)
            assertEquals(first, restarted.getRecoveryBackupInfo()?.file)
            restarted.stageRecovery(pkg(5))
            assertTrue("Selected fallback source must survive staging the replacement recovery", first.exists())
            assertFalse("Corrupt completed generation can be pruned", second.exists())
        }
    }

    @Test fun incompleteGenerationsAreProtectedAndRetentionIsBounded() = fixture { context ->
        val manager = BackupFileManager(context, Dispatchers.IO)
        val first = manager.stageRecovery(pkg(1))
        val duplicate = serializer.serialize(serializer.deserialize(pkg(1).payload).copy(exportedAt = 999))
        assertEquals(first, manager.stageRecovery(duplicate))
        val second = manager.stageRecovery(pkg(2))
        assertThrows(IllegalStateException::class.java) { manager.stageRecovery(pkg(3)) }
        assertTrue(first.exists()); assertTrue(second.exists())
        manager.completeRecovery(first)
        val third = manager.stageRecovery(pkg(3))
        assertFalse(first.exists()); assertTrue(second.exists()); assertTrue(third.exists())
        val count = LinkNestStorage.backupStagingDirectory(context).listFiles().orEmpty()
            .count { it.name.matches(Regex("recovery-[0-9a-f-]{36}\\.json")) }
        assertEquals(2, count)
    }

    @Test fun publicationFailureAndInvalidPayloadPreservePublishedRecovery() = fixture { context ->
        val manager = BackupFileManager(context, Dispatchers.IO)
        val first = manager.stageRecovery(pkg(1))
        manager.completeRecovery(first)
        val directory = LinkNestStorage.backupStagingDirectory(context)
        val failingStore = RecoveryBackupStore(directory, serializer) { _, _ ->
            throw java.io.IOException("injected ENOSPC during manifest publication")
        }
        assertThrows(java.io.IOException::class.java) { failingStore.stage(pkg(2)) }
        assertEquals(first, manager.getRecoveryBackupInfo()?.file)
        assertThrows(org.json.JSONException::class.java) { manager.stageRecovery(pkg(2).copy(payload = "truncated")) }
        assertEquals(first, manager.getRecoveryBackupInfo()?.file)
    }

    @Test fun atomicLegacyAndRecoveryManifestBackupsRecoverBeforeDiscovery() = fixture { context ->
        val manager = BackupFileManager(context, Dispatchers.IO)
        val legacy = manager.stageBackup(pkg(1))
        assertTrue(legacy.renameTo(File(legacy.path + ".bak")))
        legacy.writeText("interrupted")
        assertEquals(pkg(1).payload, manager.getStagedBackup()?.readText())
        val recovery = manager.stageRecovery(pkg(2))
        val index = File(LinkNestStorage.backupStagingDirectory(context), "recovery-index.json")
        assertTrue(index.renameTo(File(index.path + ".bak")))
        index.writeText("interrupted")
        assertEquals(recovery, BackupFileManager(context, Dispatchers.IO).getRecoveryBackupInfo()?.file)
    }

    @Test fun restartReconciliationReleasesOnlyProvenOutcomes() = fixture { context ->
        val directory = LinkNestStorage.backupStagingDirectory(context)
        fun snapshot(number: Long) = serializer.deserialize(pkg(number).payload)
        var store = RecoveryBackupStore(directory, serializer)
        val original = snapshot(1)
        val incoming = snapshot(2)
        val staged = store.stage(pkg(1))
        store.prepare(staged.name, original, incoming)
        store = RecoveryBackupStore(directory, serializer)
        assertEquals(com.linknest.core.data.model.RecoveryOutcome.ORIGINAL_PRESENT,
            store.reconcile(original).single().outcome)
        val next = store.stage(pkg(2))
        store.prepare(next.name, incoming, snapshot(3))
        store = RecoveryBackupStore(directory, serializer)
        assertEquals(com.linknest.core.data.model.RecoveryOutcome.IMPORTED_PRESENT,
            store.reconcile(snapshot(3)).single().outcome)
        val committed = store.stage(pkg(3))
        store.prepare(committed.name, snapshot(3), snapshot(4))
        store.committed(committed.name)
        store = RecoveryBackupStore(directory, serializer)
        assertEquals(com.linknest.core.data.model.RecoveryOutcome.COMMIT_RECORDED,
            store.reconcile(snapshot(5)).single().outcome)
    }

    @Test fun changedUnknownLibraryKeepsProtectionAndRejectsUnrelatedOperationToken() = fixture { context ->
        val directory = LinkNestStorage.backupStagingDirectory(context)
        var store = RecoveryBackupStore(directory, serializer)
        fun snapshot(number: Long) = serializer.deserialize(pkg(number).payload)
        val original = store.stage(pkg(1))
        store.prepare(original.name, snapshot(1), snapshot(2))
        store = RecoveryBackupStore(directory, serializer)
        assertEquals(com.linknest.core.data.model.RecoveryOutcome.UNKNOWN, store.reconcile(snapshot(3)).single().outcome)
        val another = store.stage(pkg(3))
        store.prepare(another.name, snapshot(3), snapshot(4))
        assertTrue(store.reconcile(snapshot(5)).all { it.outcome == com.linknest.core.data.model.RecoveryOutcome.UNKNOWN })
        assertThrows(IllegalStateException::class.java) { store.stage(pkg(6)) }
        assertTrue(original.exists()); assertTrue(another.exists())
        assertThrows(IllegalStateException::class.java) { store.prepare("../unowned", snapshot(1), snapshot(2)) }
    }

    @Test fun omittedCustomReferencesNeverProveOriginalLibraryEquality() = fixture { context ->
        val directory = LinkNestStorage.backupStagingDirectory(context)
        val original = serializer.deserialize(pkg(1).payload).copy(omittedCustomImages = 1)
        val store = RecoveryBackupStore(directory, serializer)
        val staged = store.stage(serializer.serialize(original))
        store.prepare(staged.name, original, serializer.deserialize(pkg(2).payload))
        val restarted = RecoveryBackupStore(directory, serializer)
        assertEquals(com.linknest.core.data.model.RecoveryOutcome.UNKNOWN, restarted.reconcile(original).single().outcome)
        assertEquals(com.linknest.core.data.model.RecoveryOutcome.UNKNOWN,
            restarted.reconcile(original.copy(omittedCustomImages = 0)).single().outcome)
        assertTrue(staged.exists())
    }

    private fun fixture(test: (ContextWrapper) -> Unit) {
        val real = InstrumentationRegistry.getInstrumentation().targetContext
        val owned = File(real.cacheDir, "recovery-test-${UUID.randomUUID()}")
        check(owned.mkdir())
        val context = object : ContextWrapper(real) { override fun getFilesDir(): File = owned }
        try { test(context) } finally { check(owned.deleteRecursively()) }
    }
}
