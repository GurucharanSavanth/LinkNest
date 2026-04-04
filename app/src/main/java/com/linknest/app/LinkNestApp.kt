package com.linknest.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.linknest.app.navigation.LinkNestNavHost
import com.linknest.core.designsystem.theme.LinkNestTheme

@Composable
fun LinkNestApp() {
    LinkNestTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            LinkNestNavHost()
        }
    }
}
