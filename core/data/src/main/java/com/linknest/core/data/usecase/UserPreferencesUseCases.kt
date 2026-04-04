package com.linknest.core.data.usecase

import com.linknest.core.data.repository.UserPreferencesRepository
import com.linknest.core.model.LayoutMode
import com.linknest.core.model.TileDensityMode
import com.linknest.core.model.UserPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveUserPreferencesUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<UserPreferences> = userPreferencesRepository.observeUserPreferences()
}

class UpdateLayoutModeUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(layoutMode: LayoutMode) {
        userPreferencesRepository.setLayoutMode(layoutMode)
    }
}

class UpdateTileSizeUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(tileSizeDp: Int) {
        userPreferencesRepository.setTileSizeDp(tileSizeDp)
    }
}

class UpdateTileDensityModeUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(tileDensityMode: TileDensityMode) {
        userPreferencesRepository.setTileDensityMode(tileDensityMode)
    }
}

class UpdateBackgroundHealthChecksUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(enabled: Boolean) {
        userPreferencesRepository.setBackgroundHealthChecksEnabled(enabled)
    }
}

class UpdateEncryptedBackupsUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(enabled: Boolean) {
        userPreferencesRepository.setEncryptedBackupsEnabled(enabled)
    }
}
