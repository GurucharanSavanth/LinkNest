package com.linknest.core.data.usecase

import com.linknest.core.data.repository.CategoryRepository
import javax.inject.Inject

class SeedDefaultCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    suspend operator fun invoke() {
        categoryRepository.seedDefaultCategoryIfEmpty()
    }
}
