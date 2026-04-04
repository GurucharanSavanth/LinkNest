package com.linknest.core.data.usecase

import com.linknest.core.data.model.BackupSnapshot
import com.linknest.core.data.model.ImportSummary
import com.linknest.core.data.repository.BackupRepository
import com.linknest.core.data.repository.CategoryRepository
import com.linknest.core.model.WidgetLink
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class ExportDataUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    suspend operator fun invoke(): BackupSnapshot = backupRepository.exportSnapshot()
}

class ImportDataUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    suspend operator fun invoke(snapshot: BackupSnapshot): ImportSummary =
        backupRepository.importSnapshot(snapshot)
}

class GetWidgetLinksUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    suspend operator fun invoke(limit: Int = 4): List<WidgetLink> =
        categoryRepository.observeDashboardCategories()
            .first()
            .flatMap { it.websites }
            .filter { it.isPinned }
            .sortedByDescending { it.openCount }
            .take(limit)
            .map { item ->
                WidgetLink(
                    websiteId = item.id,
                    title = item.title,
                    normalizedUrl = item.normalizedUrl,
                )
            }
}
