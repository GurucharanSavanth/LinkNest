package com.linknest.core.action.action

import com.linknest.core.action.ActionResult
import com.linknest.core.action.AppAction
import com.linknest.core.action.actionResult
import com.linknest.core.data.backup.BackupManager
import com.linknest.core.data.model.ImportSummary
import com.linknest.core.data.usecase.ImportDataUseCase
import javax.inject.Inject

class ImportDataAction @Inject constructor(
    private val backupManager: BackupManager,
    private val importDataUseCase: ImportDataUseCase,
) : AppAction<String, ImportSummary> {
    override suspend fun invoke(input: String): ActionResult<ImportSummary> = actionResult(
        code = "IMPORT_FAILED",
        defaultMessage = "Unable to import backup data.",
    ) {
        val snapshot = backupManager.parse(input)
        importDataUseCase(snapshot)
    }
}
