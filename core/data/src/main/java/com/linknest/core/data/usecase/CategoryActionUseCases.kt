package com.linknest.core.data.usecase

import com.linknest.core.data.repository.CategoryRepository
import com.linknest.core.data.repository.DomainCategoryMappingRepository
import com.linknest.core.data.model.CategoryUpsertRequest
import com.linknest.core.model.CategorySuggestion
import com.linknest.core.model.CategoryDraft
import com.linknest.core.model.IconType
import com.linknest.core.model.SelectableCategory
import javax.inject.Inject

class GetCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    suspend operator fun invoke(categoryId: Long): SelectableCategory? =
        categoryRepository.getCategoryById(categoryId)
}

class SuggestCategoryUseCase @Inject constructor(
    private val mappingRepository: DomainCategoryMappingRepository,
) {
    suspend operator fun invoke(
        domain: String,
        contextHint: String? = null,
    ): CategorySuggestion? = mappingRepository.suggestCategory(
        domain = domain,
        contextHint = contextHint,
    )
}

class ReorderCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    suspend operator fun invoke(orderedIds: List<Long>) {
        categoryRepository.reorderCategories(orderedIds)
    }
}

class CreateCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    suspend operator fun invoke(draft: CategoryDraft): SelectableCategory =
        categoryRepository.upsertCategory(
            CategoryUpsertRequest(
                name = draft.name,
                colorHex = draft.colorHex,
                iconType = draft.iconType,
                iconValue = draft.iconValue,
            ),
        )
}

class UpdateCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    suspend operator fun invoke(
        categoryId: Long,
        name: String,
        colorHex: String,
        iconValue: String?,
        iconType: IconType = IconType.EMOJI,
    ): SelectableCategory = categoryRepository.upsertCategory(
        CategoryUpsertRequest(
            categoryId = categoryId,
            name = name,
            colorHex = colorHex,
            iconType = iconType,
            iconValue = iconValue,
        ),
    )
}

class ArchiveCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    suspend operator fun invoke(categoryId: Long, archived: Boolean) {
        categoryRepository.archiveCategory(categoryId, archived)
    }
}

class DeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    suspend operator fun invoke(categoryId: Long) {
        categoryRepository.deleteCategory(categoryId)
    }
}
