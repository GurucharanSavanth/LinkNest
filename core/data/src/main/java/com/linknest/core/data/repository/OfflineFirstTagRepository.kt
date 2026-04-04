package com.linknest.core.data.repository

import androidx.room.withTransaction
import com.linknest.core.data.mapper.asTagModel
import com.linknest.core.database.LinkNestDatabase
import com.linknest.core.database.dao.TagDao
import com.linknest.core.database.entity.TagEntity
import com.linknest.core.database.entity.WebsiteTagCrossRefEntity
import com.linknest.core.model.TagModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstTagRepository @Inject constructor(
    private val database: LinkNestDatabase,
    private val tagDao: TagDao,
    private val searchIndexRepository: SearchIndexRepository,
) : TagRepository {
    override fun observeTags(): Flow<List<TagModel>> =
        tagDao.observeTags().map { rows -> rows.map { it.asTagModel() } }

    override suspend fun searchTags(query: String, limit: Int): List<TagModel> {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isBlank()) return emptyList()
        return tagDao.searchTags(
            queryLike = "%$normalizedQuery%",
            limit = limit,
        ).map { row -> row.asTagModel() }
    }

    override suspend fun ensureTags(names: List<String>): List<TagModel> {
        val resolvedTags = database.withTransaction {
            val result = mutableListOf<TagModel>()
            names.map(String::trim)
                .filter(String::isNotEmpty)
                .distinctBy(String::lowercase)
                .forEach { rawName ->
                    val normalizedName = rawName.replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase() else char.toString()
                    }
                    val existing = tagDao.getTagByName(normalizedName)
                    val tagId = existing?.id ?: run {
                        val insertedId = tagDao.insertTag(TagEntity(name = normalizedName))
                        if (insertedId > 0) insertedId else tagDao.getTagByName(normalizedName)?.id ?: 0L
                    }
                    if (tagId > 0) {
                        result += TagModel(id = tagId, name = normalizedName)
                    }
                }
            result
        }
        resolvedTags.forEach { tag -> searchIndexRepository.indexTag(tag.id) }
        return resolvedTags
    }

    override suspend fun assignTagsToWebsite(websiteId: Long, tagIds: List<Long>) {
        database.withTransaction {
            tagDao.deleteTagsForWebsite(websiteId)
            if (tagIds.isNotEmpty()) {
                tagDao.insertCrossRefs(
                    tagIds.distinct().map { tagId ->
                        WebsiteTagCrossRefEntity(
                            websiteId = websiteId,
                            tagId = tagId,
                        )
                    },
                )
            }
        }
        searchIndexRepository.indexWebsite(websiteId)
    }

    override suspend fun removeTagFromWebsite(websiteId: Long, tagId: Long) {
        tagDao.deleteTagFromWebsite(
            websiteId = websiteId,
            tagId = tagId,
        )
        tagDao.deleteTagIfUnused(tagId)
        searchIndexRepository.indexWebsite(websiteId)
        val remaining = tagDao.getAllTags().any { it.id == tagId }
        if (remaining) {
            searchIndexRepository.indexTag(tagId)
        } else {
            searchIndexRepository.removeTag(tagId)
        }
    }

    override suspend fun getTagNamesForWebsite(websiteId: Long): List<String> =
        tagDao.getTagNamesForWebsite(websiteId)
}
