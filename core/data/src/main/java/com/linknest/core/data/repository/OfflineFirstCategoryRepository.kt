package com.linknest.core.data.repository

import androidx.room.withTransaction
import com.linknest.core.common.time.TimeProvider
import com.linknest.core.data.mapper.asDashboardCategory
import com.linknest.core.data.mapper.asSelectableCategory
import com.linknest.core.data.model.CategoryUpsertRequest
import com.linknest.core.database.LinkNestDatabase
import com.linknest.core.database.dao.CategoryDao
import com.linknest.core.database.dao.IconCacheDao
import com.linknest.core.database.dao.TagDao
import com.linknest.core.database.entity.CategoryEntity
import com.linknest.core.model.DashboardCategory
import com.linknest.core.model.IconType
import com.linknest.core.model.SelectableCategory
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class OfflineFirstCategoryRepository @Inject constructor(
    private val database: LinkNestDatabase,
    private val categoryDao: CategoryDao,
    private val websiteDao: com.linknest.core.database.dao.WebsiteDao,
    private val iconCacheDao: IconCacheDao,
    private val tagDao: TagDao,
    private val timeProvider: TimeProvider,
    private val searchIndexRepository: SearchIndexRepository,
) : CategoryRepository {
    override fun observeArchivedCategories(): Flow<List<SelectableCategory>> =
        categoryDao.observeArchivedCategories().map { it.map(CategoryEntity::asSelectableCategory) }

    override fun observeDashboardCategories(): Flow<List<DashboardCategory>> = combine(
        categoryDao.observeCategoryWithWebsites(),
        iconCacheDao.observeAll(),
        tagDao.observeWebsiteTagAssignments(),
    ) { categories, iconCaches, tagAssignments ->
        val iconCacheByWebsiteId = iconCaches.associateBy { it.websiteId }
        val tagNamesByWebsiteId = tagAssignments.groupBy(
            keySelector = { it.websiteId },
            valueTransform = { it.tagName },
        )
        val duplicates = com.linknest.core.data.mapper.duplicateCounts(categories.flatMap { it.websites })
        categories.map { category ->
            category.asDashboardCategory(
                iconCacheByWebsiteId = iconCacheByWebsiteId,
                tagNamesByWebsiteId = tagNamesByWebsiteId,
            ).let { mapped -> mapped.copy(websites = mapped.websites.map { it.copy(duplicateCount = duplicates[it.id] ?: 0) }) }
        }
    }

    override fun observeSelectableCategories(): Flow<List<SelectableCategory>> =
        categoryDao.observeCategories().map { categories ->
            categories.map(CategoryEntity::asSelectableCategory)
        }

    override suspend fun getCategoryById(categoryId: Long): SelectableCategory? =
        categoryDao.getCategoryById(categoryId)?.asSelectableCategory()

    override suspend fun getAllCategories(): List<SelectableCategory> =
        categoryDao.getActiveCategories().map(CategoryEntity::asSelectableCategory)

    override suspend fun seedDefaultCategoryIfEmpty() {
        if (categoryDao.countCategories() > 0) return

        val now = timeProvider.now()
        val categoryId = categoryDao.insertCategory(
            CategoryEntity(
                name = "Inbox",
                colorHex = "#7C4DFF",
                iconType = IconType.EMOJI,
                iconValue = "\uD83D\uDCC1",
                sortOrder = 0,
                isCollapsed = false,
                isArchived = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
        searchIndexRepository.indexCategory(categoryId)
    }

    override suspend fun toggleCollapsed(categoryId: Long) {
        categoryDao.toggleCollapsed(
            categoryId = categoryId,
            updatedAt = timeProvider.now(),
        )
    }

    override suspend fun reorderCategories(orderedIds: List<Long>) {
        val now = timeProvider.now()
        database.withTransaction {
            orderedIds.forEachIndexed { index, categoryId ->
                categoryDao.updateSortOrder(
                    categoryId = categoryId,
                    sortOrder = index,
                    updatedAt = now,
                )
            }
        }
    }

    override suspend fun upsertCategory(request: CategoryUpsertRequest): SelectableCategory {
        val normalizedName = request.name.trim()
        require(normalizedName.isNotBlank()) { "Category name is required." }

        val now = timeProvider.now()
        val categoryId = if (request.categoryId == null) {
            categoryDao.insertCategory(
                CategoryEntity(
                    name = normalizedName,
                    colorHex = request.colorHex,
                    iconType = request.iconType,
                    iconValue = request.iconValue,
                    sortOrder = categoryDao.maxSortOrder() + 1,
                    isCollapsed = false,
                    isArchived = false,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        } else {
            categoryDao.updateCategory(
                categoryId = request.categoryId,
                name = normalizedName,
                colorHex = request.colorHex,
                iconType = request.iconType,
                iconValue = request.iconValue,
                updatedAt = now,
            )
            request.categoryId
        }

        searchIndexRepository.indexCategory(categoryId)

        return categoryDao.getCategoryById(categoryId)?.asSelectableCategory()
            ?: error("Category could not be resolved after save.")
    }

    override suspend fun archiveCategory(categoryId: Long, archived: Boolean) {
        categoryDao.setArchived(
            categoryId = categoryId,
            archived = archived,
            updatedAt = timeProvider.now(),
        )
        if (archived) {
            searchIndexRepository.removeCategory(categoryId)
        } else {
            searchIndexRepository.indexCategory(categoryId)
        }
        searchIndexRepository.reindexCategoryWebsites(categoryId)
    }

    override suspend fun deleteCategory(categoryId: Long) {
        val deletedIds = database.withTransaction {
            val ids = websiteDao.getWebsitesByCategory(categoryId).map { it.id }
            // Clear icon_cache rows for this category's websites before the FK cascade removes the
            // websites themselves (icon_cache has no FK, so it would otherwise leave orphan rows).
            iconCacheDao.deleteByCategoryId(categoryId)
            categoryDao.deleteCategory(categoryId)
            ids
        }
        deletedIds.forEach { searchIndexRepository.removeWebsite(it) }
        searchIndexRepository.removeCategory(categoryId)
    }
}
