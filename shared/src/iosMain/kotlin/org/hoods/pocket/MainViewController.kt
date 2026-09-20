package org.hoods.pocket

import androidx.compose.ui.window.ComposeUIViewController
import org.hoods.pocket.di.initKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
    }
) {
    App()
}