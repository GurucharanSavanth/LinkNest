package com.linknest.core.action.action

import com.linknest.core.action.ActionResult
import com.linknest.core.action.AppAction
import com.linknest.core.action.actionResult
import com.linknest.core.data.usecase.ArchiveCategoryUseCase
import com.linknest.core.data.usecase.DeleteCategoryUseCase
import com.linknest.core.data.usecase.DeleteWebsiteUseCase
import com.linknest.core.data.usecase.MoveWebsiteToCategoryUseCase
import com.linknest.core.data.usecase.SetWebsitePinnedUseCase
import com.linknest.core.data.usecase.UpdateCategoryUseCase
import javax.inject.Inject

data class MoveWebsiteToCategoryInput(
    val websiteId: Long,
    val targetCategoryId: Long,
)

data class SetWebsitePinnedInput(
    val websiteId: Long,
    val pinned: Boolean,
)

data class UpdateCategoryInput(
    val categoryId: Long,
    val name: String,
    val colorHex: String,
    val iconValue: String?,
)

data class ArchiveCategoryInput(
    val categoryId: Long,
    val archived: Boolean,
)

class MoveWebsiteToCategoryAction @Inject constructor(
    private val moveWebsiteToCategoryUseCase: MoveWebsiteToCategoryUseCase,
) : AppAction<MoveWebsiteToCategoryInput, Unit> {
    override suspend fun invoke(input: MoveWebsiteToCategoryInput): ActionResult<Unit> = actionResult(
        code = "MOVE_WEBSITE_FAILED",
        defaultMessage = "Unable to move website.",
    ) {
        moveWebsiteToCategoryUseCase(input.websiteId, input.targetCategoryId)
    }
}

class DeleteWebsiteAction @Inject constructor(
    private val deleteWebsiteUseCase: DeleteWebsiteUseCase,
) : AppAction<Long, Unit> {
    override suspend fun invoke(input: Long): ActionResult<Unit> = actionResult(
        code = "DELETE_WEBSITE_FAILED",
        defaultMessage = "Unable to delete website.",
    ) {
        deleteWebsiteUseCase(input)
    }
}

class SetWebsitePinnedAction @Inject constructor(
    private val setWebsitePinnedUseCase: SetWebsitePinnedUseCase,
) : AppAction<SetWebsitePinnedInput, Unit> {
    override suspend fun invoke(input: SetWebsitePinnedInput): ActionResult<Unit> = actionResult(
        code = "PIN_WEBSITE_FAILED",
        defaultMessage = "Unable to update pinned state.",
    ) {
        setWebsitePinnedUseCase(input.websiteId, input.pinned)
    }
}

class UpdateCategoryAction @Inject constructor(
    private val updateCategoryUseCase: UpdateCategoryUseCase,
) : AppAction<UpdateCategoryInput, Unit> {
    override suspend fun invoke(input: UpdateCategoryInput): ActionResult<Unit> = actionResult(
        code = "UPDATE_CATEGORY_FAILED",
        defaultMessage = "Unable to update category.",
    ) {
        updateCategoryUseCase(
            categoryId = input.categoryId,
            name = input.name,
            colorHex = input.colorHex,
            iconValue = input.iconValue,
        )
    }
}

class ArchiveCategoryAction @Inject constructor(
    private val archiveCategoryUseCase: ArchiveCategoryUseCase,
) : AppAction<ArchiveCategoryInput, Unit> {
    override suspend fun invoke(input: ArchiveCategoryInput): ActionResult<Unit> = actionResult(
        code = "ARCHIVE_CATEGORY_FAILED",
        defaultMessage = "Unable to archive category.",
    ) {
        archiveCategoryUseCase(input.categoryId, input.archived)
    }
}

class DeleteCategoryAction @Inject constructor(
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
) : AppAction<Long, Unit> {
    override suspend fun invoke(input: Long): ActionResult<Unit> = actionResult(
        code = "DELETE_CATEGORY_FAILED",
        defaultMessage = "Unable to delete category.",
    ) {
        deleteCategoryUseCase(input)
    }
}
