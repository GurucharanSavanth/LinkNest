package com.linknest.core.data.usecase

import com.linknest.core.data.repository.CategoryRepository
import com.linknest.core.data.repository.WebsiteRepository
import com.linknest.core.model.CategoryWidgetSnapshot
import com.linknest.core.model.WidgetLink
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class GetRecentWidgetLinksUseCase @Inject constructor(
    private val websiteRepository: WebsiteRepository,
) {
    suspend operator fun invoke(limit: Int = 4): List<WidgetLink> =
        websiteRepository.getRecent(limit).map { item ->
            WidgetLink(
                websiteId = item.id,
                title = item.title,
                normalizedUrl = item.normalizedUrl,
            )
        }
}

class GetCategoryWidgetSnapshotUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    suspend operator fun invoke(
        categoryId: Long? = null,
        limit: Int = 4,
    ): CategoryWidgetSnapshot {
        val categories = categoryRepository.observeDashboardCategories().first()
        val category = categories.firstOrNull { candidate ->
            candidate.id == categoryId && candidate.websites.isNotEmpty()
        } ?: categories.firstOrNull { it.websites.isNotEmpty() }

        return CategoryWidgetSnapshot(
            categoryId = category?.id,
            categoryName = category?.name ?: "Category",
            links = category?.websites?.take(limit)?.map { item ->
                WidgetLink(
                    websiteId = item.id,
                    title = item.title,
                    normalizedUrl = item.normalizedUrl,
                )
            }.orEmpty(),
        )
    }
}
