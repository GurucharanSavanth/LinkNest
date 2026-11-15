package com.linknest.core.data.backup

import com.linknest.core.data.model.BackupSnapshot
import com.linknest.core.data.model.BackupTag
import org.junit.Test

class BackupValidationTest {
    private val empty = BackupSnapshot(schemaVersion = 6, exportedAt = 1, categories = emptyList(),
        websites = emptyList(), tags = emptyList(), websiteTags = emptyList(), mappings = emptyList())

    @Test fun acceptsEmptyLibrary() { validateBackup(empty) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicateTagNamesBeforeDatabaseWrites() {
        validateBackup(empty.copy(tags = listOf(BackupTag(1, "Tag"), BackupTag(2, "Tag"))))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidIdentity() { validateBackup(empty.copy(tags = listOf(BackupTag(0, "Tag")))) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsupportedSchema() { validateBackup(empty.copy(schemaVersion = 7)) }
}
