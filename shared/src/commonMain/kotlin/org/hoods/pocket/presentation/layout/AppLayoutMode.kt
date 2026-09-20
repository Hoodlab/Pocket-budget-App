package org.hoods.pocket.presentation.layout

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

enum class AppLayoutMode {
    Compact,
    Medium,
    Expanded;

    val isExpanded: Boolean
        get() = this == Expanded

    val isMedium: Boolean
        get() = this == Medium

    val isCompact: Boolean
        get() = this == Compact

    companion object {
        fun from(widthSizeClass: WindowWidthSizeClass): AppLayoutMode =
            when (widthSizeClass) {
                WindowWidthSizeClass.Compact -> Compact
                WindowWidthSizeClass.Medium -> Medium
                WindowWidthSizeClass.Expanded -> Expanded
                else -> Compact
            }

        fun fromWidth(width: Dp): AppLayoutMode = when {
            width < 600.dp -> Compact
            width < 840.dp -> Medium
            else -> Expanded
        }

        @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
        @Composable
        fun current(): AppLayoutMode {
            val explicit = LocalAppLayoutMode.current
            if (explicit != null) return explicit

            val windowInfo = LocalWindowInfo.current
            val density = LocalDensity.current
            val containerSize = windowInfo.containerSize
            if (containerSize.width > 0 && containerSize.height > 0) {
                val size = with(density) {
                    DpSize(containerSize.width.toDp(), containerSize.height.toDp())
                }
                val windowSizeClass = WindowSizeClass.calculateFromSize(size)
                return from(windowSizeClass.widthSizeClass)
            }
            return Compact
        }
    }
}

val LocalAppLayoutMode = compositionLocalOf<AppLayoutMode?> { null }
