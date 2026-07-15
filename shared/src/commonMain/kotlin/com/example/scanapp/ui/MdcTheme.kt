package com.example.scanapp.ui

import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color

internal object MdcTheme {

    object Colors {
        val primary = Color(0xff1565C0L)
        val onPrimary = Color.WHITE
        val primaryContainer = Color(0xffD1E4FFL)
        val onPrimaryContainer = Color(0xff001D36L)
        val secondary = Color(0xff00897BL)
        val onSecondary = Color.WHITE
        val secondaryContainer = Color(0xffB2DFDBL)
        val onSecondaryContainer = Color(0xff00201EL)
        val error = Color(0xffC62828L)
        val onError = Color.WHITE
        val errorContainer = Color(0xffFFDAD6L)
        val background = Color(0xffFAFAFAL)
        val onBackground = Color(0xff1C1B1FL)
        val surface = Color.WHITE
        val onSurface = Color(0xff1C1B1FL)
        val surfaceVariant = Color(0xffF5F5F5L)
        val onSurfaceVariant = Color(0xff666666L)
        val outline = Color(0xffDADCE0L)
        val wifi = Color(0xff1565C0L)
        val bluetooth = Color(0xff00897BL)
        val cell = Color(0xff6750A4L)
        val warning = Color(0xffE65100L)
        val rankingGold = Color(0xffFF8F00L)
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
