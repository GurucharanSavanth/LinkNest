package com.linknest.core.action.action

import com.linknest.core.action.ActionResult
import com.linknest.core.action.AppAction
import com.linknest.core.action.actionResult
import com.linknest.core.data.model.PersistedIconCache
import com.linknest.core.data.storage.IconStorageManager
import javax.inject.Inject

data class CacheIconInput(
    val sourceUrl: String?,
    val fetchedAt: Long,
)

class CacheIconAction @Inject constructor(
    private val iconStorageManager: IconStorageManager,
) : AppAction<CacheIconInput, PersistedIconCache?> {
    override suspend fun invoke(input: CacheIconInput): ActionResult<PersistedIconCache?> = actionResult(
        code = "ICON_CACHE_FAILED",
        defaultMessage = "Unable to cache icon.",
    ) {
        iconStorageManager.cacheIcon(
            sourceUrl = input.sourceUrl,
            fetchedAt = input.fetchedAt,
        )
    }
}
