package org.hoods.pocket

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import org.hoods.pocket.navigation.PocketNavHost
import org.hoods.pocket.presentation.layout.AppLayoutMode
import org.hoods.pocket.presentation.layout.LocalAppLayoutMode
import org.hoods.pocket.presentation.theme.PocketColors
import org.hoods.pocket.presentation.theme.PocketTheme
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun App() {
    PocketTheme {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(PocketColors.Surface)
                .safeContentPadding()
        ) {
            val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(maxWidth, maxHeight))
            val layoutMode = AppLayoutMode.from(windowSizeClass.widthSizeClass)
            CompositionLocalProvider(LocalAppLayoutMode provides layoutMode) {
                PocketBudgetRoot(layoutMode = layoutMode)
            }
        }
    }
}

@Composable
fun PocketBudgetRoot(
    layoutMode: AppLayoutMode = AppLayoutMode.current(),
    modifier: Modifier = Modifier
) {
    PocketNavHost(
        layoutMode = layoutMode,
        modifier = modifier
    )
}
