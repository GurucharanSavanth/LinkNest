package com.linknest.app

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import com.linknest.app.testing.SelectionTestDependencies
import com.linknest.core.data.backup.BackupMediaStore
import com.linknest.core.data.backup.BackupSerializer
import com.linknest.core.data.storage.LinkNestStorage
import com.linknest.core.model.IconSource
import dagger.hilt.android.EntryPointAccessors
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class PortableMediaExportTest {
    @Test fun exportOwnedFixtureImageForAnotherInstallation() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("portableAction") == "export")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val marker = JSONObject(File(context.cacheDir, "linknest-profiling-fixture.json").readText())
        val entry = EntryPointAccessors.fromApplication(context, SelectionTestDependencies::class.java)
        val library = entry.exportData()()
        val category = library.categories.single { it.id == marker.getLong("id") }
        check(category.name == marker.getString("name"))
        val website = library.websites.first { it.categoryId == category.id }
        check(website.domain == "profiling.invalid" && website.title.startsWith("QA link "))
        val file = File(LinkNestStorage.iconCacheDirectory(context).apply { mkdirs() }, "portable-qa-${UUID.randomUUID()}.png")
        try {
            val bitmap = Bitmap.createBitmap(2, 3, Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(Color.BLUE)
                file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally { bitmap.recycle() }
            val source = library.copy(categories = listOf(category), websites = listOf(website.copy(
                chosenIconSource = IconSource.CUSTOM, customIconUri = file.toURI().toString(), customIconAssetId = null,
            )), tags = emptyList(), websiteTags = emptyList(), mappings = emptyList(), savedFilters = emptyList(),
                events = emptyList(), recentQueries = emptyList(), iconCache = emptyList(), preferences = null, assets = emptyList())
            val portable = BackupMediaStore(context, Dispatchers.IO).attach(source)
            assertEquals(1, portable.assets.size)
            assertNull(portable.websites.single().customIconUri)
            File(context.getExternalFilesDir(null), "portable-crossinstall.json")
                .writeText(BackupSerializer().serialize(portable).payload)
        } finally { check(!file.exists() || file.delete()) }
        assertFalse(file.exists())
    }
}
