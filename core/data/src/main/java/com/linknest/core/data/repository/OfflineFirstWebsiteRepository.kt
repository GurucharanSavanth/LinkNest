package com.linknest.core.data.repository

import androidx.room.withTransaction
import com.linknest.core.common.time.TimeProvider
import com.linknest.core.data.mapper.asSearchResultItem
import com.linknest.core.data.mapper.asWebsiteListItem
import com.linknest.core.data.model.PersistWebsiteRequest
import com.linknest.core.data.model.WebsiteHealthCandidate
import com.linknest.core.data.model.WebsiteHealthUpdate
import com.linknest.core.database.LinkNestDatabase
import com.linknest.core.database.dao.IconCacheDao
import com.linknest.core.database.dao.TagDao
import com.linknest.core.database.dao.WebsiteDao
import com.linknest.core.database.entity.IconCacheEntity
import com.linknest.core.database.entity.WebsiteEntryEntity
import com.linknest.core.database.entity.WebsiteTagCrossRefEntity
import com.linknest.core.model.DuplicateCheckResult
import com.linknest.core.model.DuplicateMatch
import com.linknest.core.model.DuplicateMatchType
import com.linknest.core.model.FollowUpStatus
import com.linknest.core.model.HealthStatus
import com.linknest.core.model.WebsiteListItem
import com.linknest.core.network.UrlMetadataFetcher
import com.linknest.core.network.model.NormalizedUrl
import java.net.URI
import javax.inject.Inject
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext

class OfflineFirstWebsiteRepository @Inject constructor(
    private val database: LinkNestDatabase,
    private val websiteDao: WebsiteDao,
    private val iconCacheDao: IconCacheDao,
    private val tagDao: TagDao,
    private val metadataFetcher: UrlMetadataFetcher,
    private val timeProvider: TimeProvider,
    private val searchIndexRepository: SearchIndexRepository,
) : WebsiteRepository {
    override suspend fun getNextSortOrder(categoryId: Long): Int =
        websiteDao.maxSortOrderInCategory(categoryId) + 1

    override suspend fun addWebsite(request: PersistWebsiteRequest): Long {
        val websiteId = database.withTransaction {
            val persistedWebsiteId = if (request.existingWebsiteId != null) {
                val existing = websiteDao.getWebsiteById(request.existingWebsiteId)
                    ?: error("Website no longer exists.")
                websiteDao.updateWebsite(
                    existing.copy(
                        categoryId = request.categoryId,
                        title = request.title,
                        canonicalUrl = request.canonicalUrl,
                        finalUrl = request.finalUrl,
                        normalizedUrl = request.normalizedUrl,
                        domain = request.domain,
                        ogImageUrl = request.ogImageUrl,
                        faviconUrl = request.faviconUrl,
                        chosenIconSource = request.chosenIconSource,
                        customIconUri = request.customIconUri,
                        emojiIcon = request.emojiIcon,
                        tileSizeDp = request.tileSizeDp,
                        sortOrder = request.sortOrder,
                        isPinned = request.isPinned,
                        lastCheckedAt = request.lastCheckedAt,
                        healthStatus = request.healthStatus,
                        note = request.note,
                        reasonSaved = request.reasonSaved,
                        priority = request.priority,
                        followUpStatus = request.followUpStatus,
                        revisitAt = request.revisitAt,
                        sourceLabel = request.sourceLabel,
                        customLabel = request.customLabel,
                        updatedAt = request.updatedAt,
                    ),
                )
                request.existingWebsiteId
            } else {
                websiteDao.insertWebsite(
                    WebsiteEntryEntity(
                        categoryId = request.categoryId,
                        title = request.title,
                        canonicalUrl = request.canonicalUrl,
                        finalUrl = request.finalUrl,
                        normalizedUrl = request.normalizedUrl,
                        domain = request.domain,
                        ogImageUrl = request.ogImageUrl,
                        faviconUrl = request.faviconUrl,
                        chosenIconSource = request.chosenIconSource,
                        customIconUri = request.customIconUri,
                        emojiIcon = request.emojiIcon,
                        tileSizeDp = request.tileSizeDp,
                        sortOrder = request.sortOrder,
                        isPinned = request.isPinned,
                        openCount = 0,
                        lastOpenedAt = null,
                        lastCheckedAt = request.lastCheckedAt,
                        healthStatus = request.healthStatus,
                        note = request.note,
                        reasonSaved = request.reasonSaved,
                        priority = request.priority,
                        followUpStatus = request.followUpStatus,
                        revisitAt = request.revisitAt,
                        sourceLabel = request.sourceLabel,
                        customLabel = request.customLabel,
                        createdAt = request.createdAt,
                        updatedAt = request.updatedAt,
                    ),
                )
            }

            tagDao.deleteTagsForWebsite(persistedWebsiteId)
            if (request.tagIds.isNotEmpty()) {
                tagDao.insertCrossRefs(
                    request.tagIds.map { tagId ->
                        WebsiteTagCrossRefEntity(
                            websiteId = persistedWebsiteId,
                            tagId = tagId,
                        )
                    },
                )
            }

            request.iconCache?.let { cache ->
                iconCacheDao.upsertIconCache(
                    IconCacheEntity(
                        websiteId = persistedWebsiteId,
                        sourceUrl = cache.sourceUrl,
                        localUri = cache.localUri,
                        contentHash = cache.contentHash,
                        mimeType = cache.mimeType,
                        etag = null,
                        fetchedAt = cache.fetchedAt,
                        updatedAt = cache.updatedAt,
                    ),
                )
            }

            persistedWebsiteId
        }
        searchIndexRepository.indexWebsite(websiteId)
        return websiteId
    }

    override suspend fun search(query: String, limit: Int) =
        websiteDao.searchWebsites(
            queryLike = "%${query.lowercase()}%",
            limit = limit,
        ).map { row -> row.asSearchResultItem() }

    override suspend fun getAllWebsiteItems(): List<WebsiteListItem> =
        buildList {
            websiteDao.getAllWebsites().forEach { entity ->
                add(hydrateWebsite(entity))
            }
        }

    override suspend fun getWebsiteById(websiteId: Long): WebsiteListItem? {
        val website = websiteDao.getWebsiteById(websiteId) ?: return null
        return hydrateWebsite(website)
    }

    override suspend fun getHealthCheckCandidates(
        staleBefore: Long,
        limit: Int,
    ): List<WebsiteHealthCandidate> = websiteDao.getEntriesForHealthCheck(
        staleBefore = staleBefore,
        limit = limit,
    ).map { entry ->
        WebsiteHealthCandidate(
            websiteId = entry.id,
            normalizedUrl = entry.normalizedUrl,
            domain = entry.domain,
            title = entry.title,
        )
    }

    override suspend fun updateHealthStatuses(updates: List<WebsiteHealthUpdate>) {
        database.withTransaction {
            updates.forEach { update ->
                websiteDao.updateHealthStatus(
                    websiteId = update.websiteId,
                    healthStatus = update.status,
                    lastCheckedAt = update.checkedAt,
                    updatedAt = update.checkedAt,
                )
            }
        }
        updates.forEach { update ->
            searchIndexRepository.indexWebsite(update.websiteId)
        }
    }

    override suspend fun trackOpen(websiteId: Long) {
        val now = timeProvider.now()
        websiteDao.incrementOpenCount(
            websiteId = websiteId,
            openedAt = now,
            updatedAt = now,
        )
    }

    override suspend fun reorderWebsites(categoryId: Long, orderedIds: List<Long>) {
        val now = timeProvider.now()
        database.withTransaction {
            orderedIds.forEachIndexed { index, websiteId ->
                websiteDao.updateSortOrder(
                    websiteId = websiteId,
                    categoryId = categoryId,
                    sortOrder = index,
                    updatedAt = now,
                )
            }
        }
    }

    override suspend fun moveWebsiteToCategory(websiteId: Long, targetCategoryId: Long) {
        websiteDao.updateSortOrder(
            websiteId = websiteId,
            categoryId = targetCategoryId,
            sortOrder = getNextSortOrder(targetCategoryId),
            updatedAt = timeProvider.now(),
        )
        searchIndexRepository.indexWebsite(websiteId)
    }

    override suspend fun deleteWebsite(websiteId: Long) {
        database.withTransaction {
            iconCacheDao.deleteByWebsiteId(websiteId)
            tagDao.deleteTagsForWebsite(websiteId)
            websiteDao.deleteWebsite(websiteId)
        }
        searchIndexRepository.removeWebsite(websiteId)
    }

    override suspend fun setPinned(websiteId: Long, pinned: Boolean) {
        websiteDao.updatePinned(
            websiteId = websiteId,
            pinned = pinned,
            updatedAt = timeProvider.now(),
        )
        searchIndexRepository.indexWebsite(websiteId)
    }

    override suspend fun refreshStaleMetadata(staleBefore: Long, limit: Int): Int {
        val entries = websiteDao.getEntriesNeedingMetadataRefresh(
            staleBefore = staleBefore,
            limit = limit,
        )

        entries.forEach { entry ->
            currentCoroutineContext().ensureActive()
            val refreshed = metadataFetcher.fetch(
                normalizedUrl = NormalizedUrl(
                    rawInput = entry.normalizedUrl,
                    normalizedUrl = entry.normalizedUrl,
                    host = runCatching { URI(entry.normalizedUrl).host.orEmpty() }.getOrDefault(entry.domain),
                    domain = entry.domain,
                ),
            )
            val now = timeProvider.now()
            val healthStatus = when {
                refreshed.finalUrl != entry.normalizedUrl -> HealthStatus.REDIRECTED
                refreshed.title.contains("login", ignoreCase = true) ||
                    refreshed.finalUrl.contains("login", ignoreCase = true) -> HealthStatus.LOGIN_REQUIRED
                refreshed.faviconUrl == null && refreshed.ogImageUrl == null && refreshed.title == entry.domain ->
                    HealthStatus.UNKNOWN
                else -> HealthStatus.OK
            }

            websiteDao.updateMetadata(
                websiteId = entry.id,
                title = refreshed.title,
                canonicalUrl = refreshed.canonicalUrl,
                finalUrl = refreshed.finalUrl,
                ogImageUrl = refreshed.ogImageUrl,
                faviconUrl = refreshed.faviconUrl,
                chosenIconSource = refreshed.chosenIconSource,
                lastCheckedAt = now,
                healthStatus = healthStatus,
                updatedAt = now,
            )
            searchIndexRepository.indexWebsite(entry.id)
        }

        return entries.size
    }

    override suspend fun findDuplicates(
        normalizedUrl: String,
        finalUrl: String?,
        domain: String,
        title: String,
        excludeWebsiteId: Long?,
    ): DuplicateCheckResult {
        val matches = websiteDao.findDuplicateCandidates(
            normalizedUrl = normalizedUrl,
            finalUrl = finalUrl,
            domain = domain,
            title = title,
            excludeId = excludeWebsiteId,
        ).map { row ->
            DuplicateMatch(
                websiteId = row.websiteId,
                type = when {
                    row.normalizedUrl == normalizedUrl && row.finalUrl == finalUrl -> DuplicateMatchType.EXACT_URL
                    row.normalizedUrl == normalizedUrl -> DuplicateMatchType.NORMALIZED_URL
                    !finalUrl.isNullOrBlank() && row.finalUrl == finalUrl -> DuplicateMatchType.REDIRECTED_URL
                    row.domain == domain && row.title.equals(title, ignoreCase = true) -> DuplicateMatchType.TITLE_DOMAIN
                    else -> DuplicateMatchType.EFFECTIVE_DESTINATION
                },
                title = row.title,
                normalizedUrl = row.normalizedUrl,
                categoryId = row.categoryId,
                categoryName = row.categoryName,
            )
        }
        return DuplicateCheckResult(
            hasDuplicates = matches.isNotEmpty(),
            matches = matches,
        )
    }

    override suspend fun getRecent(limit: Int): List<WebsiteListItem> =
        hydrateEntries(websiteDao.getRecentEntries(limit))

    override suspend fun getMostUsed(limit: Int): List<WebsiteListItem> =
        hydrateEntries(websiteDao.getMostUsedEntries(limit))

    override suspend fun getNeedsAttention(limit: Int): List<WebsiteListItem> =
        hydrateEntries(websiteDao.getNeedsAttentionEntries(limit))

    override suspend fun getPotentialDuplicates(limit: Int): List<WebsiteListItem> =
        hydrateEntries(websiteDao.getPotentialDuplicateEntries(limit))

    override suspend fun getUnsorted(limit: Int): List<WebsiteListItem> =
        hydrateEntries(websiteDao.getUnsortedEntries(limit))

    override suspend fun getLastImported(limit: Int): List<WebsiteListItem> =
        hydrateEntries(websiteDao.getLastImportedEntries(limit))

    override suspend fun getIntegrityBrokenLinksCount(): Int = websiteDao.countBrokenLinks()

    override suspend fun getIntegrityDuplicateCount(): Int = websiteDao.countPotentialDuplicates()

    override suspend fun getIntegrityUnsortedCount(): Int = websiteDao.countUnsortedEntries()

    override suspend fun getLatestHealthCheckTimestamp(): Long? = websiteDao.getLatestHealthCheckTimestamp()

    private suspend fun hydrateEntries(entries: List<WebsiteEntryEntity>): List<WebsiteListItem> =
        buildList(entries.size) {
            entries.forEach { entity ->
                add(hydrateWebsite(entity))
            }
        }

    private suspend fun hydrateWebsite(entity: WebsiteEntryEntity): WebsiteListItem {
        val cache = iconCacheDao.getByWebsiteId(entity.id)
        val tags = tagDao.getTagNamesForWebsite(entity.id)
        val duplicateCount = websiteDao.findDuplicateCandidates(
            normalizedUrl = entity.normalizedUrl,
            finalUrl = entity.finalUrl,
            domain = entity.domain,
            title = entity.title,
            excludeId = entity.id,
        ).size
        return entity.asWebsiteListItem(
            cachedIconUri = cache?.localUri,
            tagNames = tags,
        ).copy(duplicateCount = duplicateCount)
    }
}
