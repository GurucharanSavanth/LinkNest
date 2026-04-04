package com.linknest.core.data.repository

import com.linknest.core.common.time.TimeProvider
import com.linknest.core.database.dao.SavedFilterDao
import com.linknest.core.database.entity.SavedFilterEntity
import com.linknest.core.model.FollowUpStatus
import com.linknest.core.model.HealthStatus
import com.linknest.core.model.SavedFilter
import com.linknest.core.model.SavedFilterSpec
import com.linknest.core.model.WebsitePriority
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class OfflineFirstSavedFilterRepository @Inject constructor(
    private val savedFilterDao: SavedFilterDao,
    private val timeProvider: TimeProvider,
    private val searchIndexRepository: SearchIndexRepository,
) : SavedFilterRepository {
    override fun observeSavedFilters(): Flow<List<SavedFilter>> =
        savedFilterDao.observeSavedFilters().map { entities -> entities.map { entity -> entity.asModel() } }

    override suspend fun getSavedFilters(): List<SavedFilter> =
        savedFilterDao.getSavedFilters().map { entity -> entity.asModel() }

    override suspend fun saveFilter(
        name: String,
        spec: SavedFilterSpec,
    ): SavedFilter {
        val now = timeProvider.now()
        val normalizedName = name.trim()
        require(normalizedName.isNotBlank()) { "Filter name is required." }
        val entity = SavedFilterEntity(
            name = normalizedName,
            specJson = encodeSpec(spec),
            createdAt = now,
            updatedAt = now,
        )
        val id = savedFilterDao.insertSavedFilter(entity)
        searchIndexRepository.indexSavedFilter(id)
        return savedFilterDao.getSavedFilterById(id)?.asModel()
            ?: error("Saved filter could not be resolved after save.")
    }

    override suspend fun deleteSavedFilter(filterId: Long) {
        savedFilterDao.deleteSavedFilter(filterId)
        searchIndexRepository.removeSavedFilter(filterId)
    }

    private fun SavedFilterEntity.asModel(): SavedFilter = SavedFilter(
        id = id,
        name = name,
        spec = decodeSpec(specJson),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun encodeSpec(spec: SavedFilterSpec): String = JSONObject().apply {
        put("query", spec.query)
        put("categoryIds", JSONArray(spec.categoryIds))
        put("tagNames", JSONArray(spec.tagNames))
        put("healthStatuses", JSONArray(spec.healthStatuses.map(HealthStatus::name)))
        put("pinnedOnly", spec.pinnedOnly)
        put("recentOnly", spec.recentOnly)
        put("followUpStatuses", JSONArray(spec.followUpStatuses.map(FollowUpStatus::name)))
        put("priorities", JSONArray(spec.priorities.map(WebsitePriority::name)))
        put("duplicatesOnly", spec.duplicatesOnly)
        put("includeNeedsAttention", spec.includeNeedsAttention)
    }.toString()

    private fun decodeSpec(specJson: String): SavedFilterSpec {
        val root = JSONObject(specJson)
        return SavedFilterSpec(
            query = root.optString("query"),
            categoryIds = root.optJSONArray("categoryIds").toLongList(),
            tagNames = root.optJSONArray("tagNames").toStringList(),
            healthStatuses = root.optJSONArray("healthStatuses").toStringList().map(HealthStatus::valueOf),
            pinnedOnly = root.optBoolean("pinnedOnly"),
            recentOnly = root.optBoolean("recentOnly"),
            followUpStatuses = root.optJSONArray("followUpStatuses").toStringList().map(FollowUpStatus::valueOf),
            priorities = root.optJSONArray("priorities").toStringList().map(WebsitePriority::valueOf),
            duplicatesOnly = root.optBoolean("duplicatesOnly"),
            includeNeedsAttention = root.optBoolean("includeNeedsAttention"),
        )
    }

    private fun JSONArray?.toLongList(): List<Long> = buildList {
        if (this@toLongList == null) return@buildList
        repeat(length()) { index -> add(getLong(index)) }
    }

    private fun JSONArray?.toStringList(): List<String> = buildList {
        if (this@toStringList == null) return@buildList
        repeat(length()) { index -> add(getString(index)) }
    }
}
