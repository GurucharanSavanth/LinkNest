package com.linknest.core.data.repository

import com.linknest.core.common.time.TimeProvider
import com.linknest.core.database.dao.RecentQueryDao
import com.linknest.core.database.entity.RecentQueryEntity
import com.linknest.core.model.RecentQuery
import com.linknest.core.network.UrlSecurityPolicy
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstRecentQueryRepository @Inject constructor(
    private val recentQueryDao: RecentQueryDao,
    private val timeProvider: TimeProvider,
) : RecentQueryRepository {
    override fun observeRecentQueries(limit: Int): Flow<List<RecentQuery>> =
        recentQueryDao.observeRecentQueries(limit).map { entities ->
            entities.map { entity -> entity.toModel() }
        }

    override suspend fun getRecentQueries(limit: Int): List<RecentQuery> =
        recentQueryDao.getRecentQueries(limit).map { entity -> entity.toModel() }

    override suspend fun saveQuery(query: String) {
        val sanitized = UrlSecurityPolicy.sanitizeSearchQuery(query)
        if (sanitized.isBlank()) return

        val now = timeProvider.now()
        val existing = recentQueryDao.getByQuery(sanitized)
        if (existing == null) {
            recentQueryDao.insertRecentQuery(
                RecentQueryEntity(
                    query = sanitized,
                    useCount = 1,
                    lastUsedAt = now,
                ),
            )
        } else {
            recentQueryDao.updateRecentQuery(
                existing.copy(
                    query = sanitized,
                    useCount = existing.useCount + 1,
                    lastUsedAt = now,
                ),
            )
        }
        recentQueryDao.trimToSize(RECENT_QUERY_LIMIT)
    }

    override suspend fun clearRecentQueries() {
        recentQueryDao.clearAll()
    }

    private fun RecentQueryEntity.toModel(): RecentQuery = RecentQuery(
        id = id,
        query = query,
        useCount = useCount,
        lastUsedAt = lastUsedAt,
    )

    private companion object {
        const val RECENT_QUERY_LIMIT = 24
    }
}
