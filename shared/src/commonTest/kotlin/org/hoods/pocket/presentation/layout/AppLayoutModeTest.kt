package org.hoods.pocket.presentation.layout

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppLayoutModeTest {

    @Test
    fun fromWindowWidthSizeClass_mapsCorrectly() {
        assertEquals(AppLayoutMode.Compact, AppLayoutMode.from(WindowWidthSizeClass.Compact))
        assertEquals(AppLayoutMode.Medium, AppLayoutMode.from(WindowWidthSizeClass.Medium))
        assertEquals(AppLayoutMode.Expanded, AppLayoutMode.from(WindowWidthSizeClass.Expanded))
    }

    @Test
    fun fromWidth_under600dp_returnsCompact() {
        assertEquals(AppLayoutMode.Compact, AppLayoutMode.fromWidth(360.dp))
        assertEquals(AppLayoutMode.Compact, AppLayoutMode.fromWidth(599.dp))
    }

    @Test
    fun fromWidth_between600dpAnd840dp_returnsMedium() {
        assertEquals(AppLayoutMode.Medium, AppLayoutMode.fromWidth(600.dp))
        assertEquals(AppLayoutMode.Medium, AppLayoutMode.fromWidth(720.dp))
        assertEquals(AppLayoutMode.Medium, AppLayoutMode.fromWidth(839.dp))
    }

    @Test
    fun fromWidth_840dpAndAbove_returnsExpanded() {
        assertEquals(AppLayoutMode.Expanded, AppLayoutMode.fromWidth(840.dp))
        assertEquals(AppLayoutMode.Expanded, AppLayoutMode.fromWidth(1280.dp))
    }

    @Test
    fun helperProperties_reflectModeAccurately() {
        assertTrue(AppLayoutMode.Compact.isCompact)
        assertFalse(AppLayoutMode.Compact.isExpanded)

        assertFalse(AppLayoutMode.Medium.isCompact)
        assertFalse(AppLayoutMode.Medium.isExpanded)

        assertFalse(AppLayoutMode.Expanded.isCompact)
        assertTrue(AppLayoutMode.Expanded.isExpanded)
    }
}
