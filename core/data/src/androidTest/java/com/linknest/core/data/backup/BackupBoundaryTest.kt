package com.linknest.core.data.backup

import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.linknest.core.data.model.BackupAsset
import com.linknest.core.data.repository.BackupRestoreException
import com.linknest.core.data.repository.OfflineFirstBackupRepository
import com.linknest.core.data.repository.SearchIndexRepository
import com.linknest.core.database.LinkNestDatabase
import com.linknest.core.datastore.LinkNestPreferencesDataSource
import java.io.File
import java.io.IOException
import java.lang.reflect.Proxy
import java.net.URI
import java.util.Base64
import java.util.UUID
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class BackupBoundaryTest {
    @androidx.test.filters.SdkSuppress(minSdkVersion = 30) // Exercises AtomicFile's .new publication path.
    @Test fun stagedExportRejectsUnpublishedWritesAndPreservesPriorExport() = runBlocking {
        fixture { f ->
            val original = BackupSerializer().serialize(f.repository.exportSnapshot())
            val staged = f.fileManager.stageBackup(original)
            val blockedNew = File(staged.path + ".new").apply { check(mkdir()) }
            val sentinel = File(blockedNew, "owned-blocker").apply { writeText("preserve fixture") }
            val replacement = BackupSerializer().serialize(f.repository.exportSnapshot().copy(appVersion = "replacement"))
            assertThrows(IOException::class.java) { f.fileManager.stageBackup(replacement) }
            assertEquals(original.payload, staged.readText())
            assertEquals("preserve fixture", sentinel.readText())
        }
        fixture { f ->
            val directory = File(f.owned, "backup-staging").apply { check(mkdir()) }
            val blockedBase = File(directory, "latest-backup.json").apply { check(mkdir()) }
            val sentinel = File(blockedBase, "owned-blocker").apply { writeText("preserve fixture") }
            val pkg = BackupSerializer().serialize(f.repository.exportSnapshot())
            assertThrows(IOException::class.java) { f.fileManager.stageBackup(pkg) }
            assertEquals("preserve fixture", sentinel.readText())
        }
    }

    @Test fun imagePublicationFailurePreservesOriginalLibraryAcrossReopen() = runBlocking {
        fixture { f ->
            val originalImage = f.addCustomImage()
            val originalBytes = originalImage.readBytes()
            val before = f.repository.exportSnapshot()
            val blocked = File(f.owned, "backup-media").apply { writeText("owned publication blocker") }
            val incoming = before.copy(websites = before.websites.map { it.copy(title = "Replacement") })
            try {
                f.repository.importSnapshot(incoming)
                fail("Blocked image publication must fail before replacing Room records")
            } catch (failure: BackupRestoreException) {
                assertTrue(failure.cause is IllegalStateException)
                assertTrue(requireNotNull(failure.message).contains("Original data was preserved"))
            }
            f.reopen()
            val after = f.repository.exportSnapshot()
            assertEquals(before.copy(exportedAt = after.exportedAt), after)
            assertArrayEquals(originalBytes, originalImage.readBytes())
            assertEquals("owned publication blocker", blocked.readText())
        }
    }

    @Test fun portableImageImportsIntoDifferentPackageWithoutSourceGrant() = runBlocking {
        org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("portableAction") == "import")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotEquals("com.linknest.app", context.packageName)
        val payload = File(context.getExternalFilesDir(null), "portable-crossinstall.json").readText()
        val snapshot = BackupSerializer().deserialize(payload)
        assertEquals(1, snapshot.assets.size)
        assertNull(snapshot.websites.single().customIconUri)
        fixture { destination ->
            destination.repository.importSnapshot(snapshot)
            destination.reopen()
            val website = destination.database.websiteDao().getAllWebsites().single()
            assertEquals(com.linknest.core.model.IconSource.CUSTOM, website.chosenIconSource)
            val imported = File(URI(website.customIconUri!!))
            assertEquals(File(destination.owned, "backup-media").canonicalFile, requireNotNull(imported.parentFile).canonicalFile)
            assertArrayEquals(Base64.getDecoder().decode(snapshot.assets.single().data), imported.readBytes())
        }
    }

    @Test fun preferenceAndIndexFailuresReportCommittedReplacement() = runBlocking {
        fixture { f ->
            f.preferenceFailure = IOException("injected preferences failure")
            f.indexFailure = IOException("injected index failure")
            val before = f.repository.exportSnapshot()
            val incoming = before.copy(websites = before.websites.map { it.copy(title = "Imported") })
            val summary = f.repository.importSnapshot(incoming)
            assertEquals("Imported", f.repository.exportSnapshot().websites.single().title)
            assertTrue(summary.warnings.any { it.contains("Library replaced, but preferences") })
            assertTrue(summary.warnings.any { it.contains("Library replaced, but search") })
            f.reopen()
            assertEquals("Imported", f.repository.exportSnapshot().websites.single().title)
        }
    }

    @Test fun postCommitCancellationPropagatesAtPreferencesAndIndexBoundaries() = runBlocking {
        for (stage in listOf("preferences", "index")) fixture { f ->
            val before = f.repository.exportSnapshot()
            val recovery = f.fileManager.stageRecovery(BackupSerializer().serialize(before))
            if (stage == "preferences") f.preferenceFailure = CancellationException("injected")
            else f.indexFailure = CancellationException("injected")
            try {
                f.repository.importSnapshot(before.copy(websites = before.websites.map { it.copy(title = "Committed $stage") }), recovery.name)
                fail("Cancellation must propagate")
            } catch (_: CancellationException) { }
            f.reopen()
            assertEquals("Committed $stage", f.repository.exportSnapshot().websites.single().title)
            assertEquals(com.linknest.core.data.model.RecoveryOutcome.COMMIT_RECORDED, f.repository.reconcileRecovery().single().outcome)
        }
    }

    @Test fun customImageSurvivesSeparateStorageWithNoSourcePathAndRestart() = runBlocking {
        fixture { source -> fixture { destination ->
            val icon = source.addCustomImage()
            val snapshot = source.repository.exportSnapshot()
            assertNull(snapshot.websites.single().customIconUri)
            assertEquals(1, snapshot.assets.size)
            val payload = BackupSerializer().serialize(snapshot).payload
            val expected = icon.readBytes()
            assertTrue(icon.delete())
            destination.repository.importSnapshot(BackupSerializer().deserialize(payload))
            destination.reopen()
            val website = destination.database.websiteDao().getAllWebsites().single()
            val imported = File(URI(website.customIconUri!!))
            assertEquals(File(destination.owned, "backup-media").canonicalFile, requireNotNull(imported.parentFile).canonicalFile)
            assertArrayEquals(expected, imported.readBytes())
            assertEquals(snapshot.assets, destination.repository.exportSnapshot().assets)
        } }
    }

    @Test fun imageFilesRollbackWithRoomAndPriorImageRecoveryRemainsPortable() = runBlocking {
        fixture { f ->
            f.addCustomImage()
            val before = f.repository.exportSnapshot()
            f.database.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_media_restore BEFORE INSERT ON website_entries WHEN NEW.title='Fail' BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
            try {
                f.repository.importSnapshot(before.copy(websites = before.websites.map { it.copy(title = "Fail") }))
                fail("Expected Room rollback")
            } catch (_: BackupRestoreException) { }
            assertEquals(before.assets, f.repository.exportSnapshot().assets)
            assertTrue(File(f.owned, "backup-media").listFiles().orEmpty().isEmpty())
            val recovered = BackupSerializer().deserialize(BackupSerializer().serialize(before).payload)
            f.repository.importSnapshot(recovered)
            f.reopen()
            assertEquals(before.assets, f.repository.exportSnapshot().assets)
        }
    }

    @Test fun exportPreservesOwnedImageReferencedByEquivalentAndroidUri() = runBlocking {
        fixture { f ->
            f.addCustomImage()
            val before = f.repository.exportSnapshot()
            f.repository.importSnapshot(before)
            val website = f.database.websiteDao().getAllWebsites().single()
            val imported = File(URI(requireNotNull(website.customIconUri)))
            val androidUri = android.net.Uri.fromFile(imported).toString()
            assertNotEquals("Fixture must use equivalent URI spellings", imported.toURI().toString(), androidUri)
            val expectedBytes = imported.readBytes()
            val orphan = File(requireNotNull(imported.parentFile), "asset-${UUID.randomUUID()}.image")
            orphan.writeText("owned unreferenced fixture")
            withContext(Dispatchers.IO) {
                f.database.openHelper.writableDatabase.execSQL(
                    "UPDATE website_entries SET custom_icon_uri=? WHERE id=?", arrayOf<Any>(androidUri, website.id),
                )
            }
            val exported = f.repository.exportSnapshot()
            assertTrue("Referenced image must survive export cleanup", imported.exists())
            assertArrayEquals(expectedBytes, imported.readBytes())
            assertEquals(before.assets, exported.assets)
            assertEquals(0, exported.omittedCustomImages)
            assertFalse("Unreferenced owned image must still be cleaned", orphan.exists())
            f.reopen()
            assertEquals(before.assets, f.repository.exportSnapshot().assets)
        }
    }

    @Test fun malformedImageAndForeignPathNeverReadOrReplaceLibrary() = runBlocking {
        fixture { f ->
            val before = f.repository.exportSnapshot()
            val bad = byteArrayOf(1, 2, 3)
            val asset = BackupAsset(bad.assetChecksum(), "image/png", Base64.getEncoder().encodeToString(bad))
            val incoming = before.copy(assets = listOf(asset), websites = before.websites.map { it.copy(customIconAssetId = asset.id) })
            try { f.repository.importSnapshot(incoming); fail("Invalid image must fail") }
            catch (failure: BackupRestoreException) { assertTrue(failure.cause is IllegalArgumentException) }
            assertEquals(before.websites, f.repository.exportSnapshot().websites)
            f.database.openHelper.writableDatabase.execSQL("UPDATE website_entries SET custom_icon_uri='file:///data/foreign/private.png', chosen_icon_source='CUSTOM'")
            val exported = f.repository.exportSnapshot()
            assertEquals(1, exported.omittedCustomImages)
            assertTrue(exported.assets.isEmpty())
            assertNull(exported.websites.single().customIconUri)
        }
    }

    private suspend fun fixture(block: suspend (Fixture) -> Unit) {
        val f = Fixture()
        try { withContext(Dispatchers.IO) { f.seed() }; block(f) }
        finally { f.database.close(); f.scope.coroutineContext[Job]!!.cancelAndJoin(); check(f.owned.deleteRecursively()) }
    }

    private class Fixture {
        private val real = InstrumentationRegistry.getInstrumentation().targetContext
        val owned = File(real.cacheDir, "backup-boundary-${UUID.randomUUID()}").apply { check(mkdir()) }
        private val context = object : ContextWrapper(real) { override fun getFilesDir(): File = owned }
        val fileManager = BackupFileManager(context, Dispatchers.IO)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(owned, "prefs.preferences_pb") })
        var preferenceFailure: Exception? = null
        var indexFailure: Exception? = null
        private val preferences = LinkNestPreferencesDataSource(object : DataStore<Preferences> by store {
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                preferenceFailure?.let { throw it }
                return store.updateData(transform)
            }
        }, Dispatchers.IO)
        private val index = Proxy.newProxyInstance(SearchIndexRepository::class.java.classLoader,
            arrayOf(SearchIndexRepository::class.java)) { _, method, _ ->
            check(method.name == "rebuildIndex")
            indexFailure?.let { throw it }
            Unit
        } as SearchIndexRepository
        var database = openDatabase()
        private fun openDatabase() = Room.databaseBuilder(context, LinkNestDatabase::class.java, File(owned, "library.db").path).build()
        val repository get() = OfflineFirstBackupRepository(database, database.categoryDao(), database.websiteDao(), database.tagDao(),
            database.domainCategoryMappingDao(), database.iconCacheDao(), database.savedFilterDao(), database.integrityEventDao(),
            database.recentQueryDao(), preferences, index, BackupMediaStore(context, Dispatchers.IO), fileManager)
        fun reopen() { database.close(); database = openDatabase() }
        fun seed() {
            database.openHelper.writableDatabase.apply {
                execSQL("INSERT INTO categories VALUES (1, 'Inbox', '#000000', 'AUTO', NULL, 0, 0, 0, 1, 1)")
                execSQL("""INSERT INTO website_entries
                    (id, category_id, title, normalized_url, domain, chosen_icon_source, sort_order,
                    is_pinned, open_count, health_status, priority, follow_up_status, created_at, updated_at)
                    VALUES (1, 1, 'Original', 'https://example.com/', 'example.com', 'GENERATED', 0, 0, 0, 'UNKNOWN', 'NORMAL', 'NONE', 1, 1)""")
            }
        }
        suspend fun addCustomImage(): File = withContext(Dispatchers.IO) {
            val dir = File(owned, "icon-cache").apply { check(mkdirs()) }
            val file = File(dir, "source.png")
            val bitmap = Bitmap.createBitmap(2, 3, Bitmap.Config.ARGB_8888)
            try { file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) } }
            finally { bitmap.recycle() }
            database.openHelper.writableDatabase.execSQL("UPDATE website_entries SET custom_icon_uri=?, chosen_icon_source='CUSTOM'", arrayOf(file.toURI().toString()))
            file
        }
    }
}
