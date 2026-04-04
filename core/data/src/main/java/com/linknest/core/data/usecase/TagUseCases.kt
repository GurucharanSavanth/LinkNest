package com.linknest.core.data.usecase

import com.linknest.core.data.repository.TagRepository
import com.linknest.core.model.TagModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveTagsUseCase @Inject constructor(
    private val tagRepository: TagRepository,
) {
    operator fun invoke(): Flow<List<TagModel>> = tagRepository.observeTags()
}

class AddTagUseCase @Inject constructor(
    private val tagRepository: TagRepository,
) {
    suspend operator fun invoke(name: String): TagModel? =
        tagRepository.ensureTags(listOf(name)).firstOrNull()
}

class EnsureTagsUseCase @Inject constructor(
    private val tagRepository: TagRepository,
) {
    suspend operator fun invoke(names: List<String>): List<TagModel> =
        tagRepository.ensureTags(names)
}

class AssignTagToWebsiteUseCase @Inject constructor(
    private val tagRepository: TagRepository,
) {
    suspend operator fun invoke(
        websiteId: Long,
        tagIds: List<Long>,
    ) {
        tagRepository.assignTagsToWebsite(websiteId, tagIds)
    }
}

class RemoveTagFromWebsiteUseCase @Inject constructor(
    private val tagRepository: TagRepository,
) {
    suspend operator fun invoke(
        websiteId: Long,
        tagId: Long,
    ) {
        tagRepository.removeTagFromWebsite(websiteId, tagId)
    }
}

class FilterByTagUseCase @Inject constructor(
    private val tagRepository: TagRepository,
) {
    suspend operator fun invoke(
        query: String,
        limit: Int = 12,
    ): List<TagModel> = tagRepository.searchTags(query = query, limit = limit)
}
