package com.linknest.core.data.repository

import androidx.room.withTransaction
import com.linknest.core.data.model.BackupCategory
import com.linknest.core.data.model.BackupIconCache
import com.linknest.core.data.model.BackupIntegrityEvent
import com.linknest.core.data.model.BackupPreferences
import com.linknest.core.data.model.BackupRecentQuery
import com.linknest.core.data.model.BackupSavedFilter
import com.linknest.core.data.model.BackupSnapshot
import com.linknest.core.data.model.BackupTag
import com.linknest.core.data.model.BackupWebsite
import com.linknest.core.data.model.BackupWebsiteTag
import com.linknest.core.data.model.DomainCategoryMapping
import com.linknest.core.data.model.ImportSummary
import com.linknest.core.datastore.LinkNestPreferencesDataSource
import com.linknest.core.database.LinkNestDatabase
import com.linknest.core.database.dao.CategoryDao
import com.linknest.core.database.dao.DomainCategoryMappingDao
import com.linknest.core.database.dao.IconCacheDao
import com.linknest.core.database.dao.IntegrityEventDao
import com.linknest.core.database.dao.RecentQueryDao
import com.linknest.core.database.dao.SavedFilterDao
import com.linknest.core.database.dao.TagDao
import com.linknest.core.database.dao.WebsiteDao
import com.linknest.core.database.entity.CategoryEntity
import com.linknest.core.database.entity.DomainCategoryMappingEntity
import com.linknest.core.database.entity.IconCacheEntity
import com.linknest.core.database.entity.IntegrityEventEntity
import com.linknest.core.database.entity.RecentQueryEntity
import com.linknest.core.database.entity.SavedFilterEntity
import com.linknest.core.database.entity.TagEntity
import com.linknest.core.database.entity.WebsiteEntryEntity
import com.linknest.core.database.entity.WebsiteTagCrossRefEntity
import com.linknest.core.model.LayoutMode
import com.linknest.core.model.TileDensityMode
import com.linknest.core.model.UserPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class BackupRestoreException(message: String, cause: Throwable? = null) : Exception(message, cause)

class OfflineFirstBackupRepository @Inject constructor(
    private val database: LinkNestDatabase,
    private val categoryDao: CategoryDao,
    private val websiteDao: WebsiteDao,
    private val tagDao: TagDao,
    private val mappingDao: DomainCategoryMappingDao,
    private val iconCacheDao: IconCacheDao,
    private val savedFilterDao: SavedFilterDao,
    private val integrityEventDao: IntegrityEventDao,
    private val recentQueryDao: RecentQueryDao,
    private val preferencesDataSource: LinkNestPreferencesDataSource,
    private val searchIndexRepository: SearchIndexRepository,
) : BackupRepository {
    override suspend fun exportSnapshot(): BackupSnapshot {
        val preferences = preferencesDataSource.userPreferences.first()
        return database.withTransaction {
            val categories = categoryDao.getAllCategories()
            val websites = websiteDao.getAllWebsites()
            val tags = tagDao.getAllTags()
            val crossRefs = tagDao.getAllCrossRefs()
            val mappings = mappingDao.getAllMappings()
            val iconCache = iconCacheDao.getAll()
            val savedFilters = savedFilterDao.getSavedFilters()
            val events = integrityEventDao.getAllEvents()
            val recentQueries = recentQueryDao.getAllRecentQueries()

            BackupSnapshot(
                schemaVersion = CURRENT_DATABASE_SCHEMA_VERSION,
                exportedAt = System.currentTimeMillis(),
                categories = categories.map { entity ->
                    BackupCategory(
                        id = entity.id,
                        name = entity.name,
                        colorHex = entity.colorHex,
                        iconType = entity.iconType,
                        iconValue = entity.iconValue,
                        sortOrder = entity.sortOrder,
                        isCollapsed = entity.isCollapsed,
                        isArchived = entity.isArchived,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt,
                    )
                },
                websites = websites.map { entity ->
                    BackupWebsite(
                        id = entity.id,
                        categoryId = entity.categoryId,
                        title = entity.title,
                        canonicalUrl = entity.canonicalUrl,
                        finalUrl = entity.finalUrl,
                        normalizedUrl = entity.normalizedUrl,
                        domain = entity.domain,
                        ogImageUrl = entity.ogImageUrl,
                        faviconUrl = entity.faviconUrl,
                        chosenIconSource = entity.chosenIconSource,
                        customIconUri = entity.customIconUri,
                        emojiIcon = entity.emojiIcon,
                        tileSizeDp = entity.tileSizeDp,
                        sortOrder = entity.sortOrder,
                        isPinned = entity.isPinned,
                        openCount = entity.openCount,
                        lastOpenedAt = entity.lastOpenedAt,
                        lastCheckedAt = entity.lastCheckedAt,
                        healthStatus = entity.healthStatus,
                        note = entity.note,
                        reasonSaved = entity.reasonSaved,
                        priority = entity.priority,
                        followUpStatus = entity.followUpStatus,
                        revisitAt = entity.revisitAt,
                        sourceLabel = entity.sourceLabel,
                        customLabel = entity.customLabel,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt,
                    )
                },
                tags = tags.map { entity -> BackupTag(id = entity.id, name = entity.name) },
                websiteTags = crossRefs.map { crossRef ->
                    BackupWebsiteTag(
                        websiteId = crossRef.websiteId,
                        tagId = crossRef.tagId,
                    )
                },
                mappings = mappings.map { entity ->
                    DomainCategoryMapping(
                        domain = entity.domain,
                        categoryId = entity.categoryId,
                        usageCount = entity.usageCount,
                        lastUsedAt = entity.lastUsedAt,
                    )
                },
                savedFilters = savedFilters.map { filter ->
                    BackupSavedFilter(
                        id = filter.id,
                        name = filter.name,
                        specJson = filter.specJson,
                        createdAt = filter.createdAt,
                        updatedAt = filter.updatedAt,
                    )
                },
                events = events.map { event ->
                    BackupIntegrityEvent(
                        id = event.id,
                        type = event.type,
                        title = event.title,
                        summary = event.summary,
                        successful = event.successful,
                        createdAt = event.createdAt,
                    )
                },
                iconCache = iconCache.map { icon ->
                    BackupIconCache(
                        id = icon.id,
                        websiteId = icon.websiteId,
                        sourceUrl = icon.sourceUrl,
                        localUri = icon.localUri,
                        contentHash = icon.contentHash,
                        mimeType = icon.mimeType,
                        etag = icon.etag,
                        fetchedAt = icon.fetchedAt,
                        updatedAt = icon.updatedAt,
                    )
                },
                recentQueries = recentQueries.map { recentQuery ->
                    BackupRecentQuery(
                        id = recentQuery.id,
                        query = recentQuery.query,
                        useCount = recentQuery.useCount,
                        lastUsedAt = recentQuery.lastUsedAt,
                    )
                },
                preferences = BackupPreferences(
                    layoutMode = preferences.layoutMode.name,
                    tileSizeDp = preferences.tileSizeDp,
                    tileDensityMode = preferences.tileDensityMode.name,
                    backgroundHealthChecksEnabled = preferences.backgroundHealthChecksEnabled,
                    encryptedBackupsEnabled = preferences.encryptedBackupsEnabled,
                ),
            )
        }
    }

    override suspend fun importSnapshot(snapshot: BackupSnapshot): ImportSummary {
        require(snapshot.schemaVersion <= CURRENT_DATABASE_SCHEMA_VERSION) {
            "Backup schema version ${snapshot.schemaVersion} is newer than supported schema $CURRENT_DATABASE_SCHEMA_VERSION."
        }

        val warnings = mutableListOf<String>()
        if (snapshot.schemaVersion < CURRENT_DATABASE_SCHEMA_VERSION) {
            warnings.add("Backup schema is older than current database; compatible restore was attempted.")
        }

        validateForeignKeys(snapshot)

        val summary = try {
            database.withTransaction {
                clearForRestore()
                restoreCategories(snapshot.categories)
                restoreTags(snapshot.tags)
                restoreWebsites(snapshot.websites)
                restoreWebsiteTags(snapshot.websiteTags)
                restoreMappings(snapshot.mappings)
                restoreSavedFilters(snapshot.savedFilters)
                restoreEvents(snapshot.events)
                restoreIconCache(snapshot.iconCache)
                restoreRecentQueries(snapshot.recentQueries)

                ImportSummary(
                    importedCategories = snapshot.categories.size,
                    importedWebsites = snapshot.websites.size,
                    importedTags = snapshot.tags.size,
                    importedMappings = snapshot.mappings.size,
                    importedSavedFilters = snapshot.savedFilters.size,
                    importedEvents = snapshot.events.size,
                    importedIconCacheEntries = snapshot.iconCache.size,
                    importedRecentQueries = snapshot.recentQueries.size,
                    skippedWebsites = 0,
                )
            }
        } catch (e: Exception) {
            throw BackupRestoreException("Import failed: ${e.message}. Original data was preserved.", e)
        }

        snapshot.preferences?.let { preferences ->
            try {
                preferencesDataSource.replaceUserPreferences(preferences.toUserPreferences())
            } catch (e: Exception) {
                warnings.add("Failed to restore preferences: ${e.message}")
            }
        } ?: warnings.add("Backup did not include preferences; existing preferences were preserved.")

        try {
            searchIndexRepository.rebuildIndex()
        } catch (e: Exception) {
            warnings.add("Failed to rebuild search index: ${e.message}")
        }
        return summary.copy(warnings = warnings)
    }

    private fun validateForeignKeys(snapshot: BackupSnapshot) {
        val categoryIds = snapshot.categories.map { it.id }.toSet()
        val tagIds = snapshot.tags.map { it.id }.toSet()
        val websiteIds = snapshot.websites.map { it.id }.toSet()

        val invalidWebsiteCategoryIds = snapshot.websites
            .map { it.categoryId }
            .filter { it !in categoryIds }
            .distinct()

        if (invalidWebsiteCategoryIds.isNotEmpty()) {
            throw BackupRestoreException(
                "Invalid category references in backup: categories ${invalidWebsiteCategoryIds} do not exist."
            )
        }

        val invalidWebsiteTagRefs = snapshot.websiteTags.filter { ref ->
            ref.tagId !in tagIds || ref.websiteId !in websiteIds
        }
        if (invalidWebsiteTagRefs.isNotEmpty()) {
            throw BackupRestoreException(
                "Invalid tag references in backup: ${invalidWebsiteTagRefs.size} tag associations reference non-existent tags or websites."
            )
        }

        val invalidMappingCategoryIds = snapshot.mappings
            .map { it.categoryId }
            .filter { it !in categoryIds }
            .distinct()

        if (invalidMappingCategoryIds.isNotEmpty()) {
            throw BackupRestoreException(
                "Invalid category references in domain mappings: categories ${invalidMappingCategoryIds} do not exist."
            )
        }
    }

    private suspend fun clearForRestore() {
        recentQueryDao.clearAll()
        integrityEventDao.deleteAllEvents()
        savedFilterDao.deleteAllSavedFilters()
        iconCacheDao.deleteAll()
        tagDao.deleteAllCrossRefs()
        mappingDao.deleteAllMappings()
        websiteDao.deleteAllWebsites()
        tagDao.deleteAllTags()
        categoryDao.deleteAllCategories()
    }

    private suspend fun restoreCategories(categories: List<BackupCategory>) {
        categories.sortedBy { it.id }.forEach { category ->
            categoryDao.insertCategory(
                CategoryEntity(
                    id = category.id,
                    name = category.name,
                    colorHex = category.colorHex,
                    iconType = category.iconType,
                    iconValue = category.iconValue,
                    sortOrder = category.sortOrder,
                    isCollapsed = category.isCollapsed,
                    isArchived = category.isArchived,
                    createdAt = category.createdAt,
                    updatedAt = category.updatedAt,
                ),
            )
        }
    }

    private suspend fun restoreTags(tags: List<BackupTag>) {
        tags.sortedBy { it.id }.forEach { tag ->
            tagDao.insertTag(TagEntity(id = tag.id, name = tag.name))
        }
    }

    private suspend fun restoreWebsites(websites: List<BackupWebsite>) {
        websites.sortedBy { it.id }.forEach { website ->
            websiteDao.insertWebsite(
                WebsiteEntryEntity(
                    id = website.id,
                    categoryId = website.categoryId,
                    title = website.title,
                    canonicalUrl = website.canonicalUrl,
                    finalUrl = website.finalUrl,
                    normalizedUrl = website.normalizedUrl,
                    domain = website.domain,
                    ogImageUrl = website.ogImageUrl,
                    faviconUrl = website.faviconUrl,
                    chosenIconSource = website.chosenIconSource,
                    customIconUri = website.customIconUri,
                    emojiIcon = website.emojiIcon,
                    tileSizeDp = website.tileSizeDp,
                    sortOrder = website.sortOrder,
                    isPinned = website.isPinned,
                    openCount = website.openCount,
                    lastOpenedAt = website.lastOpenedAt,
                    lastCheckedAt = website.lastCheckedAt,
                    healthStatus = website.healthStatus,
                    note = website.note,
                    reasonSaved = website.reasonSaved,
                    priority = website.priority,
                    followUpStatus = website.followUpStatus,
                    revisitAt = website.revisitAt,
                    sourceLabel = website.sourceLabel,
                    customLabel = website.customLabel,
                    createdAt = website.createdAt,
                    updatedAt = website.updatedAt,
                ),
            )
        }
    }

    private suspend fun restoreWebsiteTags(websiteTags: List<BackupWebsiteTag>) {
        if (websiteTags.isNotEmpty()) {
            tagDao.insertCrossRefs(
                websiteTags.map { ref ->
                    WebsiteTagCrossRefEntity(
                        websiteId = ref.websiteId,
                        tagId = ref.tagId,
                    )
                },
            )
        }
    }

    private suspend fun restoreMappings(mappings: List<DomainCategoryMapping>) {
        mappings.forEach { mapping ->
            mappingDao.upsertMapping(
                DomainCategoryMappingEntity(
                    domain = mapping.domain,
                    categoryId = mapping.categoryId,
                    usageCount = mapping.usageCount,
                    lastUsedAt = mapping.lastUsedAt,
                ),
            )
        }
    }

    private suspend fun restoreSavedFilters(savedFilters: List<BackupSavedFilter>) {
        savedFilters.sortedBy { it.id }.forEach { filter ->
            savedFilterDao.insertSavedFilter(
                SavedFilterEntity(
                    id = filter.id,
                    name = filter.name,
                    specJson = filter.specJson,
                    createdAt = filter.createdAt,
                    updatedAt = filter.updatedAt,
                ),
            )
        }
    }

    private suspend fun restoreEvents(events: List<BackupIntegrityEvent>) {
        events.sortedBy { it.id }.forEach { event ->
            integrityEventDao.insertEvent(
                IntegrityEventEntity(
                    id = event.id,
                    type = event.type,
                    title = event.title,
                    summary = event.summary,
                    successful = event.successful,
                    createdAt = event.createdAt,
                ),
            )
        }
    }

    private suspend fun restoreIconCache(iconCache: List<BackupIconCache>) {
        iconCache.sortedBy { it.id }.forEach { icon ->
            iconCacheDao.upsertIconCache(
                IconCacheEntity(
                    id = icon.id,
                    websiteId = icon.websiteId,
                    sourceUrl = icon.sourceUrl,
                    localUri = icon.localUri,
                    contentHash = icon.contentHash,
                    mimeType = icon.mimeType,
                    etag = icon.etag,
                    fetchedAt = icon.fetchedAt,
                    updatedAt = icon.updatedAt,
                ),
            )
        }
    }

    private suspend fun restoreRecentQueries(recentQueries: List<BackupRecentQuery>) {
        recentQueries.sortedBy { it.id }.forEach { recentQuery ->
            recentQueryDao.insertRecentQuery(
                RecentQueryEntity(
                    id = recentQuery.id,
                    query = recentQuery.query,
                    useCount = recentQuery.useCount,
                    lastUsedAt = recentQuery.lastUsedAt,
                ),
            )
        }
    }

    private fun BackupPreferences.toUserPreferences(): UserPreferences =
        UserPreferences(
            layoutMode = runCatching { LayoutMode.valueOf(layoutMode) }.getOrDefault(LayoutMode.LIST),
            tileSizeDp = tileSizeDp,
            tileDensityMode = runCatching { TileDensityMode.valueOf(tileDensityMode) }.getOrDefault(TileDensityMode.ADAPTIVE),
            backgroundHealthChecksEnabled = backgroundHealthChecksEnabled,
            encryptedBackupsEnabled = encryptedBackupsEnabled,
        )

    private companion object {
        const val CURRENT_DATABASE_SCHEMA_VERSION = 6
    }
}
