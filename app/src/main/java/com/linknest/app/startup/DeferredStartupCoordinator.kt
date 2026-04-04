package com.linknest.app.startup

import com.linknest.app.HealthWorkScheduler
import com.linknest.app.shortcut.ShortcutPublisher
import com.linknest.core.data.usecase.ObserveUserPreferencesUseCase
import com.linknest.core.data.usecase.WarmSearchIndexUseCase
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
    private val warmSearchIndexUseCase: WarmSearchIndexUseCase,
    private val shortcutPublisher: ShortcutPublisher,
) {
    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            delay(DEFERRED_INIT_DELAY_MILLIS)
            warmSearchIndexUseCase()
            shortcutPublisher.publishDynamicShortcuts()
            observeUserPreferencesUseCase()
                .map { preferences -> preferences.backgroundHealthChecksEnabled }
                .distinctUntilChanged()
                .collectLatest { backgroundHealthChecksEnabled ->
                    healthWorkScheduler.sync(backgroundHealthChecksEnabled)
                }
        }
    }

    private companion object {
        const val DEFERRED_INIT_DELAY_MILLIS = 650L
    }
}
