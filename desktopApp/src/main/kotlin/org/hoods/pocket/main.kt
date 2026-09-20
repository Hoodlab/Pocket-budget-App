package org.hoods.pocket

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.hoods.pocket.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "pocket-budget",
        ) {
            App()
        }
    }
}