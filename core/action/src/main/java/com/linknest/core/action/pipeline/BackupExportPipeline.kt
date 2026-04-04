package com.linknest.core.action.pipeline

import com.linknest.core.action.ActionResult
import com.linknest.core.action.action.ExportDataAction
import com.linknest.core.action.model.BackupExportPipelineInput
import com.linknest.core.action.model.BackupExportPipelineOutput
import javax.inject.Inject

class BackupExportPipeline @Inject constructor(
    private val exportDataAction: ExportDataAction,
) {
    suspend operator fun invoke(input: BackupExportPipelineInput): ActionResult<BackupExportPipelineOutput> =
        when (val result = exportDataAction(input.encrypted)) {
            is ActionResult.Success -> ActionResult.Success(BackupExportPipelineOutput(result.value))
            is ActionResult.PartialSuccess -> ActionResult.PartialSuccess(
                value = BackupExportPipelineOutput(result.value),
                issues = result.issues,
            )
            is ActionResult.Failure -> result
        }
}
