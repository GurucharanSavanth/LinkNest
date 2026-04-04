package com.linknest.core.data.usecase

import com.linknest.core.data.repository.CategoryRepository
import javax.inject.Inject

class ToggleCategoryCollapsedUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    suspend operator fun invoke(categoryId: Long) {
        categoryRepository.toggleCollapsed(categoryId)
    }
}
