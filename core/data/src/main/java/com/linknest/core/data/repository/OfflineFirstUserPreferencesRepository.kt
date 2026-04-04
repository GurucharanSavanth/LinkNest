package com.linknest.core.data.repository

import com.linknest.core.datastore.LinkNestPreferencesDataSource
import com.linknest.core.model.LayoutMode
import com.linknest.core.model.TileDensityMode
import com.linknest.core.model.UserPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class OfflineFirstUserPreferencesRepository @Inject constructor(
    private val preferencesDataSource: LinkNestPreferencesDataSource,
) : UserPreferencesRepository {
    override fun observeUserPreferences(): Flow<UserPreferences> = preferencesDataSource.userPreferences

    override suspend fun setLayoutMode(layoutMode: LayoutMode) {
        preferencesDataSource.setLayoutMode(layoutMode)
    }

    override suspend fun setTileSizeDp(tileSizeDp: Int) {
        preferencesDataSource.setTileSizeDp(tileSizeDp)
    }

    override suspend fun setTileDensityMode(tileDensityMode: TileDensityMode) {
        preferencesDataSource.setTileDensityMode(tileDensityMode)
    }

    override suspend fun setBackgroundHealthChecksEnabled(enabled: Boolean) {
        preferencesDataSource.setBackgroundHealthChecksEnabled(enabled)
    }

    override suspend fun setEncryptedBackupsEnabled(enabled: Boolean) {
        preferencesDataSource.setEncryptedBackupsEnabled(enabled)
    }
}
