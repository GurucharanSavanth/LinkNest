package com.linknest.core.data.usecase

import com.linknest.core.data.repository.RecentQueryRepository
import com.linknest.core.data.repository.SearchIndexRepository
import com.linknest.core.model.RecentQuery
import com.linknest.core.model.SearchSuggestion
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class WarmSearchIndexUseCase @Inject constructor(
    private val searchIndexRepository: SearchIndexRepository,
) {
    suspend operator fun invoke() {
        searchIndexRepository.warmIndex()
    }
}

class RebuildSearchIndexUseCase @Inject constructor(
    private val searchIndexRepository: SearchIndexRepository,
) {
    suspend operator fun invoke() {
        searchIndexRepository.rebuildIndex()
    }
}

class GetSearchSuggestionsUseCase @Inject constructor(
    private val searchIndexRepository: SearchIndexRepository,
) {
    suspend operator fun invoke(
        query: String,
        limit: Int = 8,
    ): List<SearchSuggestion> = searchIndexRepository.suggestions(query, limit)
}

class SaveRecentQueryUseCase @Inject constructor(
    private val recentQueryRepository: RecentQueryRepository,
) {
    suspend operator fun invoke(query: String) {
        recentQueryRepository.saveQuery(query)
    }
}

class ObserveRecentQueriesUseCase @Inject constructor(
    private val recentQueryRepository: RecentQueryRepository,
) {
    operator fun invoke(limit: Int = 8): Flow<List<RecentQuery>> =
        recentQueryRepository.observeRecentQueries(limit)
}

class ClearRecentQueriesUseCase @Inject constructor(
    private val recentQueryRepository: RecentQueryRepository,
) {
    suspend operator fun invoke() {
        recentQueryRepository.clearRecentQueries()
    }
}

class IndexWebsiteForSearchUseCase @Inject constructor(
    private val searchIndexRepository: SearchIndexRepository,
) {
    suspend operator fun invoke(websiteId: Long) {
        searchIndexRepository.indexWebsite(websiteId)
    }
}

class RemoveWebsiteFromSearchIndexUseCase @Inject constructor(
    private val searchIndexRepository: SearchIndexRepository,
) {
    suspend operator fun invoke(websiteId: Long) {
        searchIndexRepository.removeWebsite(websiteId)
    }
}

class IndexCategoryForSearchUseCase @Inject constructor(
    private val searchIndexRepository: SearchIndexRepository,
) {
    suspend operator fun invoke(categoryId: Long) {
        searchIndexRepository.indexCategory(categoryId)
    }
}

class RemoveCategoryFromSearchIndexUseCase @Inject constructor(
    private val searchIndexRepository: SearchIndexRepository,
) {
    suspend operator fun invoke(categoryId: Long) {
        searchIndexRepository.removeCategory(categoryId)
    }
}

class ReindexCategoryWebsitesUseCase @Inject constructor(
    private val searchIndexRepository: SearchIndexRepository,
) {
    suspend operator fun invoke(categoryId: Long) {
        searchIndexRepository.reindexCategoryWebsites(categoryId)
    }
}

class IndexTagForSearchUseCase @Inject constructor(
    private val searchIndexRepository: SearchIndexRepository,
) {
    suspend operator fun invoke(tagId: Long) {
        searchIndexRepository.indexTag(tagId)
    }
}

class RemoveTagFromSearchIndexUseCase @Inject constructor(
    private val searchIndexRepository: SearchIndexRepository,
) {
    suspend operator fun invoke(tagId: Long) {
        searchIndexRepository.removeTag(tagId)
    }
}

class IndexSavedFilterForSearchUseCase @Inject constructor(
    private val searchIndexRepository: SearchIndexRepository,
) {
    suspend operator fun invoke(filterId: Long) {
        searchIndexRepository.indexSavedFilter(filterId)
    }
}

class RemoveSavedFilterFromSearchIndexUseCase @Inject constructor(
    private val searchIndexRepository: SearchIndexRepository,
) {
    suspend operator fun invoke(filterId: Long) {
        searchIndexRepository.removeSavedFilter(filterId)
    }
}
