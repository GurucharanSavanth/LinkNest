package com.linknest.core.data.usecase

import com.linknest.core.data.repository.CategoryRepository
import com.linknest.core.model.SelectableCategory
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveSelectableCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    operator fun invoke(): Flow<List<SelectableCategory>> = categoryRepository.observeSelectableCategories()
}
