package com.linknest.core.action.pipeline

import com.linknest.core.action.ActionResult
import com.linknest.core.action.action.ImportDataAction
import com.linknest.core.action.model.ImportRestorePipelineOutput
import javax.inject.Inject

class ImportRestorePipeline @Inject constructor(
    private val importDataAction: ImportDataAction,
) {
    suspend operator fun invoke(payload: String): ActionResult<ImportRestorePipelineOutput> =
        when (val result = importDataAction(payload)) {
            is ActionResult.Success -> ActionResult.Success(ImportRestorePipelineOutput(result.value))
            is ActionResult.PartialSuccess -> ActionResult.PartialSuccess(
                value = ImportRestorePipelineOutput(result.value),
                issues = result.issues,
            )
            is ActionResult.Failure -> result
        }
}
