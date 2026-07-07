package com.example.scanapp.ui

import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color

internal object MdcTheme {

    object Colors {
        val primary = Color("#1565C0")
        val onPrimary = Color.WHITE
        val primaryContainer = Color("#D1E4FF")
        val onPrimaryContainer = Color("#001D36")
        val secondary = Color("#00897B")
        val onSecondary = Color.WHITE
        val secondaryContainer = Color("#B2DFDB")
        val onSecondaryContainer = Color("#00201E")
        val error = Color("#C62828")
        val onError = Color.WHITE
        val errorContainer = Color("#FFDAD6")
        val background = Color("#FAFAFA")
        val onBackground = Color("#1C1B1F")
        val surface = Color.WHITE
        val onSurface = Color("#1C1B1F")
        val surfaceVariant = Color("#F5F5F5")
        val onSurfaceVariant = Color("#666666")
        val outline = Color("#DADCE0")
        val wifi = Color("#1565C0")
        val bluetooth = Color("#00897B")
        val warning = Color("#E65100")
        val rankingGold = Color("#FF8F00")
    }

    object Typography {
        val headlineLarge = 28f
        val headlineMedium = 24f
        val titleLarge = 20f
        val titleMedium = 16f
        val titleSmall = 14f
        val bodyLarge = 14f
        val bodyMedium = 13f
        val bodySmall = 12f
        val labelLarge = 14f
        val labelSmall = 11f
    }

    object Spacing {
        val xs = 4f
        val sm = 8f
        val md = 16f
        val lg = 24f
        val xl = 32f
        val card = 12f
    }

    object Elevation {
        val level0 = BoxShadow(0f, 0f, 0f, Color.TRANSPARENT)
        val level1 = BoxShadow(0f, 1f, 3f, Color(0x1F000000))
        val level2 = BoxShadow(0f, 2f, 6f, Color(0x26000000))
        val level3 = BoxShadow(0f, 4f, 8f, Color(0x2D000000))
        val level4 = BoxShadow(0f, 6f, 10f, Color(0x33000000))
    }
}
