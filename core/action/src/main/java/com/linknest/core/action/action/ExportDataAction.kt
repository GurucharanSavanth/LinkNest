package com.linknest.core.action.action

import com.linknest.core.action.ActionResult
import com.linknest.core.action.AppAction
import com.linknest.core.action.actionResult
import com.linknest.core.data.backup.BackupManager
import com.linknest.core.data.model.BackupArtifact
import com.linknest.core.data.usecase.ExportDataUseCase
import javax.inject.Inject

class ExportDataAction @Inject constructor(
    private val exportDataUseCase: ExportDataUseCase,
    private val backupManager: BackupManager,
) : AppAction<Boolean, BackupArtifact> {
    override suspend fun invoke(input: Boolean): ActionResult<BackupArtifact> = actionResult(
        code = "EXPORT_FAILED",
        defaultMessage = "Unable to export backup data.",
    ) {
        backupManager.export(
            snapshot = exportDataUseCase(),
            encrypted = input,
        )
    }
}
