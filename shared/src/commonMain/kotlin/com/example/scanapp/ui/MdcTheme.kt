package com.example.scanapp.ui

import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color

internal object MdcTheme {

    object Colors {
        val primary = Color(0xff58C7F3L)
        val onPrimary = Color(0xff002A38L)
        val primaryContainer = Color(0xff153A47L)
        val onPrimaryContainer = Color(0xffBDEBFCL)
        val secondary = Color(0xff63D5A3L)
        val onSecondary = Color(0xff003824L)
        val secondaryContainer = Color(0xff174332L)
        val onSecondaryContainer = Color(0xffB8F1D3L)
        val error = Color(0xffFF8A80L)
        val onError = Color(0xff3B0504L)
        val errorContainer = Color(0xff5A1F1DL)
        val background = Color(0xff0D0F10L)
        val onBackground = Color(0xffE7ECEEL)
        val surface = Color(0xff171A1CL)
        val onSurface = Color(0xffE7ECEEL)
        val surfaceVariant = Color(0xff23272AL)
        val onSurfaceVariant = Color(0xffAAB4B8L)
        val outline = Color(0xff3A4144L)
        val wifi = Color(0xff62B5FFL)
        val bluetooth = Color(0xff64D8CBL)
        val cell = Color(0xffD0B4FFL)
        val warning = Color(0xffFFBE5CL)
        val rankingGold = Color(0xffFFD166L)
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
        val level1 = BoxShadow(0f, 1f, 3f, Color(0x66000000))
        val level2 = BoxShadow(0f, 2f, 6f, Color(0x73000000))
        val level3 = BoxShadow(0f, 4f, 8f, Color(0x80000000))
        val level4 = BoxShadow(0f, 6f, 10f, Color(0x8C000000))
    }
}
