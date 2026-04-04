package com.linknest.core.data.repository

import com.linknest.core.data.model.BackupSnapshot
import com.linknest.core.data.model.DomainCategoryMapping
import com.linknest.core.data.model.ImportSummary
import com.linknest.core.data.model.IntegrityEventRecord
import com.linknest.core.data.model.PersistWebsiteRequest
import com.linknest.core.data.model.SavedFilterRecord
import com.linknest.core.data.model.WebsiteHealthCandidate
import com.linknest.core.data.model.WebsiteHealthUpdate
import com.linknest.core.model.CategorySuggestion
import com.linknest.core.model.DashboardCategory
import com.linknest.core.model.DuplicateCheckResult
import com.linknest.core.model.LayoutMode
import com.linknest.core.model.RecentQuery
import com.linknest.core.model.SavedFilter
import com.linknest.core.model.SavedFilterSpec
import com.linknest.core.model.SearchSuggestion
import com.linknest.core.model.SearchResultItem
import com.linknest.core.model.SelectableCategory
import com.linknest.core.model.TagModel
import com.linknest.core.model.TileDensityMode
import com.linknest.core.model.UserPreferences
import com.linknest.core.model.WebsiteListItem
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeDashboardCategories(): Flow<List<DashboardCategory>>
    fun observeSelectableCategories(): Flow<List<SelectableCategory>>
    suspend fun getCategoryById(categoryId: Long): SelectableCategory?
    suspend fun getAllCategories(): List<SelectableCategory>
    suspend fun seedDefaultCategoryIfEmpty()
    suspend fun toggleCollapsed(categoryId: Long)
    suspend fun reorderCategories(orderedIds: List<Long>)
    suspend fun upsertCategory(request: com.linknest.core.data.model.CategoryUpsertRequest): SelectableCategory
    suspend fun archiveCategory(categoryId: Long, archived: Boolean)
    suspend fun deleteCategory(categoryId: Long)
}

interface WebsiteRepository {
    suspend fun addWebsite(request: PersistWebsiteRequest): Long
    suspend fun getNextSortOrder(categoryId: Long): Int
    suspend fun search(query: String, limit: Int = 40): List<SearchResultItem>
    suspend fun getAllWebsiteItems(): List<WebsiteListItem>
    suspend fun getWebsiteById(websiteId: Long): WebsiteListItem?
    suspend fun getHealthCheckCandidates(staleBefore: Long, limit: Int = 20): List<WebsiteHealthCandidate>
    suspend fun updateHealthStatuses(updates: List<WebsiteHealthUpdate>)
    suspend fun trackOpen(websiteId: Long)
    suspend fun reorderWebsites(categoryId: Long, orderedIds: List<Long>)
    suspend fun moveWebsiteToCategory(websiteId: Long, targetCategoryId: Long)
    suspend fun deleteWebsite(websiteId: Long)
    suspend fun setPinned(websiteId: Long, pinned: Boolean)
    suspend fun refreshStaleMetadata(staleBefore: Long, limit: Int = 20): Int
    suspend fun findDuplicates(
        normalizedUrl: String,
        finalUrl: String?,
        domain: String,
        title: String,
        excludeWebsiteId: Long? = null,
    ): DuplicateCheckResult

    suspend fun getRecent(limit: Int = 8): List<WebsiteListItem>
    suspend fun getMostUsed(limit: Int = 8): List<WebsiteListItem>
    suspend fun getNeedsAttention(limit: Int = 8): List<WebsiteListItem>
    suspend fun getPotentialDuplicates(limit: Int = 8): List<WebsiteListItem>
    suspend fun getUnsorted(limit: Int = 8): List<WebsiteListItem>
    suspend fun getLastImported(limit: Int = 8): List<WebsiteListItem>
    suspend fun getIntegrityBrokenLinksCount(): Int
    suspend fun getIntegrityDuplicateCount(): Int
    suspend fun getIntegrityUnsortedCount(): Int
    suspend fun getLatestHealthCheckTimestamp(): Long?
}

interface TagRepository {
    fun observeTags(): Flow<List<TagModel>>
    suspend fun searchTags(query: String, limit: Int = 12): List<TagModel>
    suspend fun ensureTags(names: List<String>): List<TagModel>
    suspend fun assignTagsToWebsite(websiteId: Long, tagIds: List<Long>)
    suspend fun removeTagFromWebsite(websiteId: Long, tagId: Long)
    suspend fun getTagNamesForWebsite(websiteId: Long): List<String>
}

interface DomainCategoryMappingRepository {
    suspend fun suggestCategory(
        domain: String,
        contextHint: String? = null,
    ): CategorySuggestion?

    suspend fun recordUsage(
        domain: String,
        categoryId: Long,
    )

    suspend fun getAllMappings(): List<DomainCategoryMapping>
}

interface BackupRepository {
    suspend fun exportSnapshot(): BackupSnapshot
    suspend fun importSnapshot(snapshot: BackupSnapshot): ImportSummary
}

interface SavedFilterRepository {
    fun observeSavedFilters(): Flow<List<SavedFilter>>
    suspend fun getSavedFilters(): List<SavedFilter>
    suspend fun saveFilter(name: String, spec: SavedFilterSpec): SavedFilter
    suspend fun deleteSavedFilter(filterId: Long)
}

interface RecentQueryRepository {
    fun observeRecentQueries(limit: Int = 8): Flow<List<RecentQuery>>
    suspend fun getRecentQueries(limit: Int = 8): List<RecentQuery>
    suspend fun saveQuery(query: String)
    suspend fun clearRecentQueries()
}

interface SearchIndexRepository {
    suspend fun warmIndex()
    suspend fun rebuildIndex()
    suspend fun search(query: String, limit: Int = 40): List<SearchResultItem>
    suspend fun suggestions(query: String, limit: Int = 8): List<SearchSuggestion>
    suspend fun indexWebsite(websiteId: Long)
    suspend fun removeWebsite(websiteId: Long)
    suspend fun indexCategory(categoryId: Long)
    suspend fun removeCategory(categoryId: Long)
    suspend fun indexTag(tagId: Long)
    suspend fun removeTag(tagId: Long)
    suspend fun indexSavedFilter(filterId: Long)
    suspend fun removeSavedFilter(filterId: Long)
    suspend fun reindexCategoryWebsites(categoryId: Long)
}

interface IntegrityEventRepository {
    suspend fun recordEvent(record: IntegrityEventRecord): Long
    suspend fun getRecentEvents(limit: Int = 8): List<com.linknest.core.model.IntegrityEvent>
    suspend fun getLatestEventTimestampByType(type: com.linknest.core.model.IntegrityEventType): Long?
}

interface UserPreferencesRepository {
    fun observeUserPreferences(): Flow<UserPreferences>
    suspend fun setLayoutMode(layoutMode: LayoutMode)
    suspend fun setTileSizeDp(tileSizeDp: Int)
    suspend fun setTileDensityMode(tileDensityMode: TileDensityMode)
    suspend fun setBackgroundHealthChecksEnabled(enabled: Boolean)
    suspend fun setEncryptedBackupsEnabled(enabled: Boolean)
}
