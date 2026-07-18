package com.example.scanapp.ui

import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.layout.FlexPositionType
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

// ──────────────────────────────────────────
// Typography
// ──────────────────────────────────────────

internal fun ViewContainer<*, *>.MdcTitle(text: String) {
    Text {
        attr {
            this.text(text)
            fontSize(MdcTheme.Typography.headlineMedium)
            fontWeightBold()
            color(MdcTheme.Colors.onBackground)
            marginTop(MdcTheme.Spacing.sm)
            marginBottom(MdcTheme.Spacing.xs)
        }
    }
}

internal fun ViewContainer<*, *>.MdcTopBar(
    title: () -> String,
    onBack: () -> Unit
) {
    View {
        attr {
            flexDirection(FlexDirection.ROW)
            alignItems(FlexAlign.CENTER)
            padding(top = MdcTheme.Spacing.xs, bottom = MdcTheme.Spacing.sm)
        }
        View {
            attr {
                width(72f)
                height(40f)
                borderRadius(20f)
                backgroundColor(MdcTheme.Colors.surface)
                alignItems(FlexAlign.CENTER)
                justifyContent(FlexJustifyContent.CENTER)
                marginRight(MdcTheme.Spacing.sm)
            }
            event {
                click { onBack() }
            }
            Text {
                attr {
                    text("Back")
                    fontSize(MdcTheme.Typography.labelLarge)
                    fontWeightSemiBold()
                    color(MdcTheme.Colors.primary)
                }
            }
        }
        Text {
            attr {
                text(title())
                flex(1f)
                fontSize(MdcTheme.Typography.headlineMedium)
                fontWeightBold()
                color(MdcTheme.Colors.onBackground)
            }
        }
    }
}

internal fun ViewContainer<*, *>.MdcMenuTopBar(
    title: String,
    onMenu: () -> Unit
) {
    View {
        attr {
            flexDirection(FlexDirection.ROW)
            alignItems(FlexAlign.CENTER)
            padding(top = MdcTheme.Spacing.xs, bottom = MdcTheme.Spacing.sm)
        }
        View {
            attr {
                width(72f)
                height(40f)
                borderRadius(20f)
                backgroundColor(MdcTheme.Colors.surface)
                alignItems(FlexAlign.CENTER)
                justifyContent(FlexJustifyContent.CENTER)
                marginRight(MdcTheme.Spacing.sm)
            }
            event {
                click { onMenu() }
            }
            Text {
                attr {
                    text("Menu")
                    fontSize(MdcTheme.Typography.labelLarge)
                    fontWeightSemiBold()
                    color(MdcTheme.Colors.primary)
                }
            }
        }
        Text {
            attr {
                text(title)
                flex(1f)
                fontSize(MdcTheme.Typography.headlineMedium)
                fontWeightBold()
                color(MdcTheme.Colors.onBackground)
            }
        }
    }
}

internal fun ViewContainer<*, *>.MdcNavigationDrawer(
    currentPage: String,
    pageHeight: Float,
    onClose: () -> Unit,
    onNavigate: (String) -> Unit
) {
    View {
        attr {
            positionType(FlexPositionType.ABSOLUTE)
            absolutePositionAllZero()
            flexDirection(FlexDirection.ROW)
            backgroundColor(Color(0x66000000))
            zIndex(20)
        }
        View {
            attr {
                size(280f, pageHeight)
                backgroundColor(MdcTheme.Colors.surface)
                padding(MdcTheme.Spacing.md)
                boxShadow(MdcTheme.Elevation.level3)
            }
            Text {
                attr {
                    text("ScanApp")
                    fontSize(MdcTheme.Typography.titleLarge)
                    fontWeightBold()
                    color(MdcTheme.Colors.onSurface)
                    marginBottom(MdcTheme.Spacing.md)
                }
            }
            MdcDrawerItem("Scanner", "Scanner", currentPage, onNavigate)
            MdcDrawerItem("Devices", "DeviceList", currentPage, onNavigate)
            MdcDrawerItem("Map", "Map", currentPage, onNavigate)
            MdcDrawerItem("Logs", "Logs", currentPage, onNavigate)
            MdcDrawerItem("Settings", "Settings", currentPage, onNavigate)
        }
        View {
            attr {
                flex(1f)
            }
            event {
                click { onClose() }
            }
        }
    }
}

internal fun ViewContainer<*, *>.MdcNavigationDrawerHost(
    isOpen: () -> Boolean,
    currentPage: () -> String,
    pageHeight: Float,
    onClose: () -> Unit,
    onNavigate: (String) -> Unit
) {
    vif({ isOpen() }) {
        MdcNavigationDrawer(
            currentPage = currentPage(),
            pageHeight = pageHeight,
            onClose = onClose,
            onNavigate = onNavigate
        )
    }
}

private fun ViewContainer<*, *>.MdcDrawerItem(
    label: String,
    pageName: String,
    currentPage: String,
    onNavigate: (String) -> Unit
) {
    val selected = pageName == currentPage
    View {
        attr {
            padding(top = 12f, bottom = 12f, left = MdcTheme.Spacing.sm, right = MdcTheme.Spacing.sm)
            marginBottom(MdcTheme.Spacing.xs)
            borderRadius(8f)
            backgroundColor(if (selected) MdcTheme.Colors.primaryContainer else Color.TRANSPARENT)
        }
        event {
            click { onNavigate(pageName) }
        }
        Text {
            attr {
                text(label)
                fontSize(MdcTheme.Typography.bodyLarge)
                fontWeightSemiBold()
                color(if (selected) MdcTheme.Colors.onPrimaryContainer else MdcTheme.Colors.onSurface)
            }
        }
    }
}

internal fun ViewContainer<*, *>.MdcRecordColumn(
    title: String,
    body: () -> String,
    accentColor: Color,
    rightMargin: Boolean = false
) {
    View {
        attr {
            flex(1f)
            flexDirection(FlexDirection.COLUMN)
            padding(MdcTheme.Spacing.card)
            backgroundColor(MdcTheme.Colors.surface)
            borderRadius(12f)
            border(Border(1f, BorderStyle.SOLID, MdcTheme.Colors.outline))
            boxShadow(MdcTheme.Elevation.level1)
            if (rightMargin) {
                marginRight(MdcTheme.Spacing.sm)
            }
        }
        Text {
            attr {
                text(title)
                fontSize(MdcTheme.Typography.titleMedium)
                fontWeightSemiBold()
                color(accentColor)
                marginBottom(MdcTheme.Spacing.sm)
            }
        }
        Text {
            attr {
                text(body())
                fontSize(MdcTheme.Typography.bodySmall)
                color(MdcTheme.Colors.onSurface)
                lineHeight(18f)
            }
        }
    }
}

internal fun ViewContainer<*, *>.MdcDeviceCard(
    title: String,
    identity: String,
    primaryMetric: String,
    secondaryMetric: String,
    count: Int,
    scanTime: String = "",
    color: Color,
    tag: String = "",
    tagColor: Color = MdcTheme.Colors.secondary,
    onClick: () -> Unit
) {
    MdcCard(onClick = onClick) {
        View {
            attr {
                flexDirection(FlexDirection.ROW)
                alignItems(FlexAlign.CENTER)
                justifyContent(FlexJustifyContent.SPACE_BETWEEN)
            }
            Text {
                attr {
                    text(title)
                    fontSize(MdcTheme.Typography.bodyLarge)
                    fontWeightSemiBold()
                    color(MdcTheme.Colors.onSurface)
                    flex(1f)
                    marginRight(MdcTheme.Spacing.sm)
                }
            }
            if (tag.isNotEmpty()) {
                MdcTag(tag, tagColor)
            }
        }
        Text {
            attr {
                text(identity)
                fontSize(MdcTheme.Typography.bodySmall)
                color(MdcTheme.Colors.onSurfaceVariant)
                lineHeight(16f)
                marginTop(2f)
            }
        }
        View {
            attr {
                flexDirection(FlexDirection.ROW)
                justifyContent(FlexJustifyContent.SPACE_BETWEEN)
                marginTop(MdcTheme.Spacing.sm)
            }
            MdcCaption(primaryMetric, color)
            MdcCaption(secondaryMetric)
        }
        MdcCaption("Seen $count times", MdcTheme.Colors.secondary)
        if (scanTime.isNotEmpty()) {
            MdcCaption("Last scanned $scanTime", MdcTheme.Colors.onSurfaceVariant)
        }
    }
}

internal fun ViewContainer<*, *>.MdcTag(
    text: String,
    color: Color
) {
    View {
        attr {
            padding(left = 8f, right = 8f, top = 3f, bottom = 3f)
            backgroundColor(MdcTheme.Colors.surfaceVariant)
            borderRadius(10f)
        }
        Text {
            attr {
                text(text)
                fontSize(MdcTheme.Typography.labelSmall)
                fontWeightMedium()
                color(color)
            }
        }
    }
}

internal fun ViewContainer<*, *>.MdcSectionHeader(text: String) {
    Text {
        attr {
            this.text(text)
            fontSize(MdcTheme.Typography.titleMedium)
            fontWeightSemiBold()
            color(MdcTheme.Colors.onSurface)
            marginTop(MdcTheme.Spacing.md)
            marginBottom(MdcTheme.Spacing.sm)
        }
    }
}

internal fun ViewContainer<*, *>.MdcBodyText(text: String, color: Color = MdcTheme.Colors.onSurface) {
    Text {
        attr {
            this.text(text)
            fontSize(MdcTheme.Typography.bodyLarge)
            this.color(color)
            marginTop(MdcTheme.Spacing.sm)
        }
    }
}

internal fun ViewContainer<*, *>.MdcBodyText(text: () -> String, color: Color = MdcTheme.Colors.onSurface) {
    Text {
        attr {
            this.text(text())
            fontSize(MdcTheme.Typography.bodyLarge)
            this.color(color)
            marginTop(MdcTheme.Spacing.sm)
        }
    }
}

internal fun ViewContainer<*, *>.MdcCaption(text: String, color: Color = MdcTheme.Colors.onSurfaceVariant) {
    Text {
        attr {
            this.text(text)
            fontSize(MdcTheme.Typography.bodySmall)
            this.color(color)
            marginTop(2f)
        }
    }
}

// ──────────────────────────────────────────
// Cards
// ──────────────────────────────────────────

internal fun ViewContainer<*, *>.MdcCard(
    elevation: BoxShadow = MdcTheme.Elevation.level1,
    onClick: (() -> Unit)? = null,
    content: ViewContainer<*, *>.() -> Unit
) {
    View {
        attr {
            flexDirection(FlexDirection.COLUMN)
            padding(MdcTheme.Spacing.card)
            backgroundColor(MdcTheme.Colors.surface)
            borderRadius(12f)
            border(Border(1f, BorderStyle.SOLID, MdcTheme.Colors.outline))
            boxShadow(elevation)
            marginTop(MdcTheme.Spacing.sm)
        }
        if (onClick != null) {
            event {
                click { onClick() }
            }
        }
        content()
    }
}

internal fun ViewContainer<*, *>.MdcCardRow(
    elevation: BoxShadow = MdcTheme.Elevation.level1,
    content: ViewContainer<*, *>.() -> Unit
) {
    View {
        attr {
            flexDirection(FlexDirection.ROW)
            padding(MdcTheme.Spacing.card)
            backgroundColor(MdcTheme.Colors.surface)
            borderRadius(12f)
            border(Border(1f, BorderStyle.SOLID, MdcTheme.Colors.outline))
            boxShadow(elevation)
            marginTop(MdcTheme.Spacing.sm)
            alignItems(FlexAlign.CENTER)
            justifyContent(FlexJustifyContent.SPACE_BETWEEN)
        }
        content()
    }
}

// ──────────────────────────────────────────
// Buttons
// ──────────────────────────────────────────

internal fun ViewContainer<*, *>.MdcFilledButton(
    label: String,
    onClick: () -> Unit
) {
    View {
        attr {
            marginTop(MdcTheme.Spacing.sm)
            padding(top = 12f, bottom = 12f, left = MdcTheme.Spacing.md + 4f, right = MdcTheme.Spacing.md + 4f)
            backgroundColor(MdcTheme.Colors.primary)
            borderRadius(20f)
            alignItems(FlexAlign.CENTER)
            justifyContent(FlexJustifyContent.CENTER)
            boxShadow(MdcTheme.Elevation.level1)
        }
        event {
            click { onClick() }
        }
        Text {
            attr {
                text(label)
                fontSize(MdcTheme.Typography.labelLarge)
                fontWeightSemiBold()
                color(MdcTheme.Colors.onPrimary)
            }
        }
    }
}

internal fun ViewContainer<*, *>.MdcOutlinedButton(
    label: String,
    onClick: () -> Unit
) {
    View {
        attr {
            marginTop(MdcTheme.Spacing.xs)
            padding(top = 10f, bottom = 10f, left = MdcTheme.Spacing.md, right = MdcTheme.Spacing.md)
            backgroundColor(Color.TRANSPARENT)
            borderRadius(20f)
            border(Border(1f, BorderStyle.SOLID, MdcTheme.Colors.outline))
            alignItems(FlexAlign.CENTER)
            justifyContent(FlexJustifyContent.CENTER)
        }
        event {
            click { onClick() }
        }
        Text {
            attr {
                text(label)
                fontSize(MdcTheme.Typography.labelLarge)
                fontWeightSemiBold()
                color(MdcTheme.Colors.primary)
            }
        }
    }
}

internal fun ViewContainer<*, *>.MdcTextButton(
    label: String,
    onClick: () -> Unit
) {
    View {
        attr {
            marginTop(MdcTheme.Spacing.xs)
            padding(top = 8f, bottom = 8f, left = MdcTheme.Spacing.sm, right = MdcTheme.Spacing.sm)
            backgroundColor(Color.TRANSPARENT)
            borderRadius(20f)
            alignItems(FlexAlign.CENTER)
            justifyContent(FlexJustifyContent.CENTER)
        }
        event {
            click { onClick() }
        }
        Text {
            attr {
                text(label)
                fontSize(MdcTheme.Typography.labelLarge)
                fontWeightSemiBold()
                color(MdcTheme.Colors.primary)
            }
        }
    }
}

internal fun ViewContainer<*, *>.MdcErrorButton(
    label: String,
    onClick: () -> Unit
) {
    View {
        attr {
            marginTop(MdcTheme.Spacing.sm)
            padding(top = 12f, bottom = 12f, left = MdcTheme.Spacing.md + 4f, right = MdcTheme.Spacing.md + 4f)
            backgroundColor(MdcTheme.Colors.error)
            borderRadius(20f)
            alignItems(FlexAlign.CENTER)
            justifyContent(FlexJustifyContent.CENTER)
        }
        event {
            click { onClick() }
        }
        Text {
            attr {
                text(label)
                fontSize(MdcTheme.Typography.labelLarge)
                fontWeightSemiBold()
                color(MdcTheme.Colors.onError)
            }
        }
    }
}

// ──────────────────────────────────────────
// List Items
// ──────────────────────────────────────────

internal fun ViewContainer<*, *>.MdcListItem(
    title: String,
    subtitle: String = "",
    trailing: String = "",
    trailingColor: Color = MdcTheme.Colors.onSurfaceVariant
) {
    View {
        attr {
            flexDirection(FlexDirection.ROW)
            justifyContent(FlexJustifyContent.SPACE_BETWEEN)
            alignItems(FlexAlign.CENTER)
            minHeight(56f)
            padding(top = MdcTheme.Spacing.sm + 2f, bottom = MdcTheme.Spacing.sm + 2f)
        }
        View {
            attr {
                flex(1f)
                flexDirection(FlexDirection.COLUMN)
                marginRight(MdcTheme.Spacing.sm)
            }
            Text {
                attr {
                    text(title.ifEmpty { "Unknown" })
                    fontSize(MdcTheme.Typography.bodyLarge)
                    fontWeightMedium()
                    color(MdcTheme.Colors.onSurface)
                    lines(1)
                    textOverFlowTail()
                }
            }
            if (subtitle.isNotEmpty()) {
                Text {
                    attr {
                        text(subtitle)
                        fontSize(MdcTheme.Typography.bodySmall)
                        color(MdcTheme.Colors.onSurfaceVariant)
                        marginTop(2f)
                        lines(1)
                        textOverFlowTail()
                    }
                }
            }
        }
        if (trailing.isNotEmpty()) {
            Text {
                attr {
                    text(trailing)
                    width(80f)
                    fontSize(MdcTheme.Typography.bodyMedium)
                    color(trailingColor)
                    fontWeightMedium()
                    textAlignRight()
                }
            }
        }
    }
}

// ──────────────────────────────────────────
// Divider
// ──────────────────────────────────────────

internal fun ViewContainer<*, *>.MdcDivider() {
    View {
        attr {
            height(1f)
            backgroundColor(MdcTheme.Colors.outline)
            marginTop(MdcTheme.Spacing.sm)
            marginBottom(MdcTheme.Spacing.sm)
        }
    }
}

// ──────────────────────────────────────────
// Tabs
// ──────────────────────────────────────────

internal fun ViewContainer<*, *>.MdcTab(
    label: String,
    selected: () -> Boolean,
    onClick: () -> Unit
) {
    View {
        attr {
            padding(top = MdcTheme.Spacing.sm, bottom = MdcTheme.Spacing.sm, left = MdcTheme.Spacing.md + 4f, right = MdcTheme.Spacing.md + 4f)
            backgroundColor(if (selected()) MdcTheme.Colors.primaryContainer else Color.TRANSPARENT)
            borderRadius(20f)
            marginRight(MdcTheme.Spacing.xs)
            alignItems(FlexAlign.CENTER)
            justifyContent(FlexJustifyContent.CENTER)
        }
        event {
            click { onClick() }
        }
        Text {
            attr {
                text(label)
                fontSize(MdcTheme.Typography.labelLarge)
                fontWeightSemiBold()
                color(if (selected()) MdcTheme.Colors.onPrimaryContainer else MdcTheme.Colors.onSurfaceVariant)
            }
        }
    }
}

// ──────────────────────────────────────────
// Badge / Stat display
// ──────────────────────────────────────────

internal fun ViewContainer<*, *>.MdcStatBadge(
    label: String,
    value: () -> String,
    color: Color
) {
    View {
        attr {
            flex(1f)
            alignItems(FlexAlign.CENTER)
        }
        Text {
            attr {
                text(value())
                fontSize(MdcTheme.Typography.titleLarge)
                fontWeightBold()
                color(color)
            }
        }
        Text {
            attr {
                text(label)
                fontSize(MdcTheme.Typography.bodySmall)
                color(MdcTheme.Colors.onSurfaceVariant)
                marginTop(2f)
            }
        }
    }
}

// ══════════════════════════════════════════
// Backward Compatible Aliases (old → new)
// ══════════════════════════════════════════

internal fun ViewContainer<*, *>.TitleText(text: String) {
    MdcTitle(text)
}

internal fun ViewContainer<*, *>.InfoText(text: String, color: Color = MdcTheme.Colors.onSurfaceVariant) {
    MdcBodyText(text, color)
}

internal fun ViewContainer<*, *>.ActionButton(
    label: String,
    background: Color,
    textColor: Color = Color.WHITE,
    onClick: () -> Unit
) {
    View {
        attr {
            marginTop(MdcTheme.Spacing.sm)
            padding(top = 12f, bottom = 12f, left = MdcTheme.Spacing.md + 4f, right = MdcTheme.Spacing.md + 4f)
            backgroundColor(background)
            borderRadius(20f)
            alignItems(FlexAlign.CENTER)
            justifyContent(FlexJustifyContent.CENTER)
        }
        event {
            click { onClick() }
        }
        Text {
            attr {
                text(label)
                fontSize(MdcTheme.Typography.labelLarge)
                fontWeightSemiBold()
                color(textColor)
            }
        }
    }
}
