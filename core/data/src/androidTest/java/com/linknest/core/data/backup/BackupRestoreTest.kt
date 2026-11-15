package com.linknest.core.data.backup

import androidx.room.Room
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.linknest.core.database.LinkNestDatabase
import com.linknest.core.datastore.LinkNestPreferencesDataSource
import com.linknest.core.data.repository.OfflineFirstBackupRepository
import com.linknest.core.data.repository.SearchIndexRepository
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.lang.reflect.Proxy

class BackupRestoreTest {
    @Test fun roundTripAndFailedReplacement_preserveOriginalRows() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, LinkNestDatabase::class.java).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val file = File(context.cacheDir, "restore-${System.nanoTime()}.preferences_pb")
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        val preferences = LinkNestPreferencesDataSource(store, Dispatchers.IO)
        val index = Proxy.newProxyInstance(SearchIndexRepository::class.java.classLoader,
            arrayOf(SearchIndexRepository::class.java)) { _, method, _ ->
            check(method.name == "rebuildIndex")
            Unit
        } as SearchIndexRepository
        val repository = OfflineFirstBackupRepository(db, db.categoryDao(), db.websiteDao(), db.tagDao(),
            db.domainCategoryMappingDao(), db.iconCacheDao(), db.savedFilterDao(), db.integrityEventDao(),
            db.recentQueryDao(), preferences, index)
        try {
            withContext(Dispatchers.IO) {
                db.openHelper.writableDatabase.apply {
                    execSQL("INSERT INTO categories VALUES (1, 'Inbox', '#000000', 'AUTO', NULL, 0, 0, 0, 1, 1)")
                    execSQL("INSERT INTO tags VALUES (1, 'Saved')")
                    execSQL("""INSERT INTO website_entries
                        (id, category_id, title, normalized_url, domain, chosen_icon_source, sort_order,
                         is_pinned, open_count, health_status, priority, follow_up_status, created_at, updated_at)
                        VALUES (1, 1, 'Original', 'https://example.com/', 'example.com', 'GENERATED', 0, 0, 0, 'UNKNOWN', 'NORMAL', 'NONE', 1, 1)""")
                    execSQL("INSERT INTO website_tag_cross_ref VALUES (1, 1)")
                }
            }
            preferences.setBackupFolderUri("content://local/folder")
            val before = repository.exportSnapshot()
            val serializer = BackupSerializer()
            val restored = serializer.deserialize(serializer.serialize(before).payload)
            repository.importSnapshot(restored)
            assertEquals(before.websites, repository.exportSnapshot().websites)
            assertEquals(before.websiteTags, repository.exportSnapshot().websiteTags)
            withContext(Dispatchers.IO) {
                db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_restore BEFORE INSERT ON website_entries WHEN NEW.title='Fail' BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
            }
            try {
                repository.importSnapshot(before.copy(websites = before.websites.map { it.copy(title = "Fail") }))
                fail("Expected transactional failure")
            } catch (_: com.linknest.core.data.repository.BackupRestoreException) { }
            assertEquals(before.websites, repository.exportSnapshot().websites)
            assertEquals(before.websiteTags, repository.exportSnapshot().websiteTags)
        } finally {
            db.close(); scope.cancel(); file.delete()
        }
    }
}
