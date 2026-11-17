package com.linknest.app

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.linknest.app.widget.HomeWidgetRefresher
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var homeWidgetRefresher: HomeWidgetRefresher

    @Inject
    lateinit var shortcutPublisher: com.linknest.app.shortcut.ShortcutPublisher

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) shortcutPublisher.reportActivation(intent)
        setContent {
            LinkNestApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        setIntent(intent)
        shortcutPublisher.reportActivation(intent)
        // Navigation can copy this intent into a new task while delivering its listeners.
        super.onNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) {
            homeWidgetRefresher.refreshAll()
        }
    }
}
