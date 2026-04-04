package com.linknest.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.linknest.core.common.coroutine.IoDispatcher
import com.linknest.core.model.LayoutMode
import com.linknest.core.model.TileDensityMode
import com.linknest.core.model.UserPreferences
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LinkNestPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    val userPreferences: Flow<UserPreferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences ->
            UserPreferences(
                layoutMode = preferences[LAYOUT_MODE]
                    ?.let { runCatching { LayoutMode.valueOf(it) }.getOrNull() }
                    ?: LayoutMode.LIST,
                tileSizeDp = preferences[TILE_SIZE_DP] ?: UserPreferences.DEFAULT_TILE_SIZE_DP,
                tileDensityMode = preferences[TILE_DENSITY_MODE]
                    ?.let { runCatching { TileDensityMode.valueOf(it) }.getOrNull() }
                    ?: TileDensityMode.ADAPTIVE,
                backgroundHealthChecksEnabled = preferences[BACKGROUND_HEALTH_CHECKS_ENABLED] ?: true,
                encryptedBackupsEnabled = preferences[ENCRYPTED_BACKUPS_ENABLED] ?: true,
            )
        }

    suspend fun setLayoutMode(layoutMode: LayoutMode) = withContext(ioDispatcher) {
        dataStore.edit { preferences ->
            preferences[LAYOUT_MODE] = layoutMode.name
        }
    }

    suspend fun setTileSizeDp(tileSizeDp: Int) = withContext(ioDispatcher) {
        dataStore.edit { preferences ->
            preferences[TILE_SIZE_DP] = tileSizeDp
        }
    }

    suspend fun setTileDensityMode(tileDensityMode: TileDensityMode) = withContext(ioDispatcher) {
        dataStore.edit { preferences ->
            preferences[TILE_DENSITY_MODE] = tileDensityMode.name
        }
    }

    suspend fun setBackgroundHealthChecksEnabled(enabled: Boolean) = withContext(ioDispatcher) {
        dataStore.edit { preferences ->
            preferences[BACKGROUND_HEALTH_CHECKS_ENABLED] = enabled
        }
    }

    suspend fun setEncryptedBackupsEnabled(enabled: Boolean) = withContext(ioDispatcher) {
        dataStore.edit { preferences ->
            preferences[ENCRYPTED_BACKUPS_ENABLED] = enabled
        }
    }

    private companion object {
        val LAYOUT_MODE = stringPreferencesKey("layout_mode")
        val TILE_SIZE_DP = intPreferencesKey("tile_size_dp")
        val TILE_DENSITY_MODE = stringPreferencesKey("tile_density_mode")
        val BACKGROUND_HEALTH_CHECKS_ENABLED = booleanPreferencesKey("background_health_checks_enabled")
        val ENCRYPTED_BACKUPS_ENABLED = booleanPreferencesKey("encrypted_backups_enabled")
    }
}
