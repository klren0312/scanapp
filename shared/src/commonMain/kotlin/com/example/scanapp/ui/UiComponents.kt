package com.example.scanapp.ui

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

internal fun ViewContainer<*, *>.TitleText(text: String) {
    Text {
        attr {
            this.text(text)
            fontSize(24f)
            marginTop(40f)
            alignSelf(FlexAlign.CENTER)
            color(Color("#333333"))
        }
    }
}

internal fun ViewContainer<*, *>.InfoText(text: String, color: Color = Color("#666666")) {
    Text {
        attr {
            this.text(text)
            fontSize(14f)
            marginTop(8f)
            this.color(color)
        }
    }
}

internal fun ViewContainer<*, *>.ActionButton(
    label: String,
    background: Color,
    textColor: Color = Color.WHITE,
    onClick: () -> Unit
) {
    View {
        attr {
            marginTop(10f)
            padding(12f)
            backgroundColor(background)
            borderRadius(8f)
            alignItems(FlexAlign.CENTER)
            justifyContent(FlexJustifyContent.CENTER)
        }
        event {
            click {
                onClick()
            }
        }
        Text {
            attr {
                text(label)
                fontSize(15f)
                color(textColor)
            }
        }
    }
}
