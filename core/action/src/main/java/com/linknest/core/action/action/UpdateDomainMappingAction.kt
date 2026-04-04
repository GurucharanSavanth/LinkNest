package com.linknest.core.action.action

import com.linknest.core.action.ActionResult
import com.linknest.core.action.AppAction
import com.linknest.core.action.actionResult
import com.linknest.core.action.model.UpdateDomainMappingInput
import com.linknest.core.data.repository.DomainCategoryMappingRepository
import javax.inject.Inject

class UpdateDomainMappingAction @Inject constructor(
    private val mappingRepository: DomainCategoryMappingRepository,
) : AppAction<UpdateDomainMappingInput, Unit> {
    override suspend fun invoke(input: UpdateDomainMappingInput): ActionResult<Unit> = actionResult(
        code = "DOMAIN_MAPPING_FAILED",
        defaultMessage = "Unable to update domain-category history.",
    ) {
        mappingRepository.recordUsage(
            domain = input.domain,
            categoryId = input.categoryId,
        )
    }
}
