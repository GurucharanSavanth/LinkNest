package com.linknest.app.startup

import com.linknest.app.HealthWorkScheduler
import com.linknest.app.shortcut.ShortcutPublisher
import com.linknest.core.data.repository.SearchIndexRepository
import com.linknest.core.data.usecase.ObserveUserPreferencesUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Singleton
class DeferredStartupCoordinator @Inject constructor(
    private val observeUserPreferencesUseCase: ObserveUserPreferencesUseCase,
    private val healthWorkScheduler: HealthWorkScheduler,
    private val shortcutPublisher: ShortcutPublisher,
    private val searchIndexRepository: SearchIndexRepository,
) {
    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            observeUserPreferencesUseCase()
                .map { preferences -> preferences.backgroundHealthChecksEnabled }
                .distinctUntilChanged()
                .collectLatest { backgroundHealthChecksEnabled ->
                    healthWorkScheduler.sync(backgroundHealthChecksEnabled)
                }
        }
        scope.launch(Dispatchers.Default) {
            delay(SHORTCUT_INIT_DELAY_MILLIS)
            runCatching { shortcutPublisher.publishDynamicShortcuts() }
        }
        scope.launch(Dispatchers.IO) {
            delay(SEARCH_WARMUP_DELAY_MILLIS)
            runCatching { searchIndexRepository.warmIndex() }
        }
    }

    private companion object {
        const val SHORTCUT_INIT_DELAY_MILLIS = 1_500L
        const val SEARCH_WARMUP_DELAY_MILLIS = 2_000L
    }
}
