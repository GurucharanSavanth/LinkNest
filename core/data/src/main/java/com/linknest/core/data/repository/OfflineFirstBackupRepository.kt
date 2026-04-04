package com.linknest.core.data.repository

import androidx.room.withTransaction
import com.linknest.core.data.model.BackupCategory
import com.linknest.core.data.model.BackupIntegrityEvent
import com.linknest.core.data.model.BackupSavedFilter
import com.linknest.core.data.model.BackupSnapshot
import com.linknest.core.data.model.BackupTag
import com.linknest.core.data.model.BackupWebsite
import com.linknest.core.data.model.BackupWebsiteTag
import com.linknest.core.data.model.DomainCategoryMapping
import com.linknest.core.data.model.ImportSummary
import com.linknest.core.database.LinkNestDatabase
import com.linknest.core.database.dao.CategoryDao
import com.linknest.core.database.dao.DomainCategoryMappingDao
import com.linknest.core.database.dao.IntegrityEventDao
import com.linknest.core.database.dao.SavedFilterDao
import com.linknest.core.database.dao.TagDao
import com.linknest.core.database.dao.WebsiteDao
import com.linknest.core.database.entity.CategoryEntity
import com.linknest.core.database.entity.DomainCategoryMappingEntity
import com.linknest.core.database.entity.IntegrityEventEntity
import com.linknest.core.database.entity.SavedFilterEntity
import com.linknest.core.database.entity.TagEntity
import com.linknest.core.database.entity.WebsiteEntryEntity
import com.linknest.core.database.entity.WebsiteTagCrossRefEntity
import java.util.Locale
import javax.inject.Inject

class OfflineFirstBackupRepository @Inject constructor(
    private val database: LinkNestDatabase,
    private val categoryDao: CategoryDao,
    private val websiteDao: WebsiteDao,
    private val tagDao: TagDao,
    private val mappingDao: DomainCategoryMappingDao,
    private val savedFilterDao: SavedFilterDao,
    private val integrityEventDao: IntegrityEventDao,
    private val searchIndexRepository: SearchIndexRepository,
) : BackupRepository {
    override suspend fun exportSnapshot(): BackupSnapshot = database.withTransaction {
        val categories = categoryDao.getActiveCategories()
        val websites = websiteDao.getAllWebsites()
        val tags = tagDao.getAllTags()
        val crossRefs = tagDao.getAllCrossRefs()
        val mappings = mappingDao.getAllMappings()
        val savedFilters = savedFilterDao.getSavedFilters()
        val events = integrityEventDao.getRecentEvents(limit = 50)

        BackupSnapshot(
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
        )
    }

    override suspend fun importSnapshot(snapshot: BackupSnapshot): ImportSummary {
        val summary = database.withTransaction {
        var importedCategories = 0
        var importedWebsites = 0
        var importedTags = 0
        var importedMappings = 0
        var importedSavedFilters = 0
        var importedEvents = 0
        var skippedWebsites = 0

        val existingCategoriesByName = categoryDao.getActiveCategories()
            .associateByTo(mutableMapOf()) { category -> category.name.caseFoldKey() }
        val categoryIdMap = mutableMapOf<Long, Long>()
        snapshot.categories.sortedBy { it.sortOrder }.forEach { backupCategory ->
            val lookupKey = backupCategory.name.caseFoldKey()
            val existing = existingCategoriesByName[lookupKey]
            val categoryId = existing?.id ?: categoryDao.insertCategory(
                CategoryEntity(
                    name = backupCategory.name,
                    colorHex = backupCategory.colorHex,
                    iconType = backupCategory.iconType,
                    iconValue = backupCategory.iconValue,
                    sortOrder = backupCategory.sortOrder,
                    isCollapsed = backupCategory.isCollapsed,
                    isArchived = backupCategory.isArchived,
                    createdAt = backupCategory.createdAt,
                    updatedAt = backupCategory.updatedAt,
                ),
            ).also { importedCategories += 1 }
            if (existing == null) {
                existingCategoriesByName[lookupKey] = CategoryEntity(
                    id = categoryId,
                    name = backupCategory.name,
                    colorHex = backupCategory.colorHex,
                    iconType = backupCategory.iconType,
                    iconValue = backupCategory.iconValue,
                    sortOrder = backupCategory.sortOrder,
                    isCollapsed = backupCategory.isCollapsed,
                    isArchived = backupCategory.isArchived,
                    createdAt = backupCategory.createdAt,
                    updatedAt = backupCategory.updatedAt,
                )
            }
            categoryIdMap[backupCategory.id] = categoryId
        }

        val existingTagsByName = tagDao.getAllTags()
            .associateByTo(mutableMapOf()) { tag -> tag.name.caseFoldKey() }
        val tagIdMap = mutableMapOf<Long, Long>()
        snapshot.tags.forEach { backupTag ->
            val lookupKey = backupTag.name.caseFoldKey()
            val existing = existingTagsByName[lookupKey]
            val tagId = existing?.id ?: tagDao.insertTag(TagEntity(name = backupTag.name)).also {
                importedTags += if (it > 0) 1 else 0
            }.takeIf { it > 0 } ?: tagDao.getTagByName(backupTag.name)?.id ?: 0L
            if (tagId > 0) {
                if (existing == null) {
                    existingTagsByName[lookupKey] = TagEntity(id = tagId, name = backupTag.name)
                }
                tagIdMap[backupTag.id] = tagId
            }
        }

        val nextSortOrdersByCategory = categoryIdMap.values
            .distinct()
            .associateWith { categoryId -> websiteDao.maxSortOrderInCategory(categoryId) + 1 }
            .toMutableMap()
        val websiteIdMap = mutableMapOf<Long, Long>()
        snapshot.websites.sortedBy { it.sortOrder }.forEach { backupWebsite ->
            val mappedCategoryId = categoryIdMap[backupWebsite.categoryId] ?: run {
                skippedWebsites += 1
                return@forEach
            }
            val existing = websiteDao.findDuplicateCandidates(
                normalizedUrl = backupWebsite.normalizedUrl,
                finalUrl = backupWebsite.finalUrl,
                domain = backupWebsite.domain,
                title = backupWebsite.title,
            ).firstOrNull { candidate ->
                candidate.isUrlDuplicateOf(backupWebsite)
            }
            if (existing != null) {
                skippedWebsites += 1
                websiteIdMap[backupWebsite.id] = existing.websiteId
                return@forEach
            }
            val nextSortOrder = nextSortOrdersByCategory.getValue(mappedCategoryId)
            val newId = websiteDao.insertWebsite(
                WebsiteEntryEntity(
                    categoryId = mappedCategoryId,
                    title = backupWebsite.title,
                    canonicalUrl = backupWebsite.canonicalUrl,
                    finalUrl = backupWebsite.finalUrl,
                    normalizedUrl = backupWebsite.normalizedUrl,
                    domain = backupWebsite.domain,
                    ogImageUrl = backupWebsite.ogImageUrl,
                    faviconUrl = backupWebsite.faviconUrl,
                    chosenIconSource = backupWebsite.chosenIconSource,
                    customIconUri = backupWebsite.customIconUri,
                    emojiIcon = backupWebsite.emojiIcon,
                    tileSizeDp = backupWebsite.tileSizeDp,
                    sortOrder = nextSortOrder,
                    isPinned = backupWebsite.isPinned,
                    openCount = backupWebsite.openCount,
                    lastOpenedAt = backupWebsite.lastOpenedAt,
                    lastCheckedAt = backupWebsite.lastCheckedAt,
                    healthStatus = backupWebsite.healthStatus,
                    note = backupWebsite.note,
                    reasonSaved = backupWebsite.reasonSaved,
                    priority = backupWebsite.priority,
                    followUpStatus = backupWebsite.followUpStatus,
                    revisitAt = backupWebsite.revisitAt,
                    sourceLabel = backupWebsite.sourceLabel,
                    customLabel = backupWebsite.customLabel,
                    createdAt = backupWebsite.createdAt,
                    updatedAt = backupWebsite.updatedAt,
                ),
            )
            nextSortOrdersByCategory[mappedCategoryId] = nextSortOrder + 1
            importedWebsites += 1
            websiteIdMap[backupWebsite.id] = newId
        }

        val importedCrossRefs = snapshot.websiteTags.mapNotNull { ref ->
            val mappedWebsiteId = websiteIdMap[ref.websiteId] ?: return@mapNotNull null
            val mappedTagId = tagIdMap[ref.tagId] ?: return@mapNotNull null
            WebsiteTagCrossRefEntity(
                websiteId = mappedWebsiteId,
                tagId = mappedTagId,
            )
        }
        if (importedCrossRefs.isNotEmpty()) {
            tagDao.insertCrossRefs(importedCrossRefs)
        }

        snapshot.mappings.forEach { mapping ->
            val mappedCategoryId = categoryIdMap[mapping.categoryId] ?: return@forEach
            mappingDao.upsertMapping(
                DomainCategoryMappingEntity(
                    domain = mapping.domain,
                    categoryId = mappedCategoryId,
                    usageCount = mapping.usageCount,
                    lastUsedAt = mapping.lastUsedAt,
                ),
            )
            importedMappings += 1
        }

        val existingSavedFiltersByName = savedFilterDao.getSavedFilters()
            .associateByTo(mutableMapOf()) { savedFilter -> savedFilter.name.caseFoldKey() }
        snapshot.savedFilters.forEach { filter ->
            val lookupKey = filter.name.caseFoldKey()
            val existing = existingSavedFiltersByName[lookupKey]
            if (existing == null) {
                val newFilter = SavedFilterEntity(
                    name = filter.name,
                    specJson = filter.specJson,
                    createdAt = filter.createdAt,
                    updatedAt = filter.updatedAt,
                )
                val filterId = savedFilterDao.insertSavedFilter(newFilter)
                existingSavedFiltersByName[lookupKey] = newFilter.copy(id = filterId)
                importedSavedFilters += 1
            }
        }

        snapshot.events.forEach { event ->
            integrityEventDao.insertEvent(
                IntegrityEventEntity(
                    type = event.type,
                    title = event.title,
                    summary = event.summary,
                    successful = event.successful,
                    createdAt = event.createdAt,
                ),
            )
            importedEvents += 1
        }

            ImportSummary(
            importedCategories = importedCategories,
            importedWebsites = importedWebsites,
            importedTags = importedTags,
            importedMappings = importedMappings,
            importedSavedFilters = importedSavedFilters,
            importedEvents = importedEvents,
            skippedWebsites = skippedWebsites,
        )
        }
        searchIndexRepository.rebuildIndex()
        return summary
    }
}

private fun String.caseFoldKey(): String = lowercase(Locale.ROOT)

private fun com.linknest.core.database.model.DuplicateWebsiteRow.isUrlDuplicateOf(
    backupWebsite: BackupWebsite,
): Boolean {
    if (normalizedUrl == backupWebsite.normalizedUrl) {
        return true
    }

    val backupFinalUrl = backupWebsite.finalUrl?.takeIf(String::isNotBlank) ?: return false
    return finalUrl == backupFinalUrl ||
        canonicalUrl == backupFinalUrl ||
        normalizedUrl == backupFinalUrl
}
