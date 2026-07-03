package com.example.scanapp.ui

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.size
import com.tencent.kuikly.core.layout.allCenter
import com.tencent.kuikly.core.layout.padding
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.base.Page
import com.tencent.kuikly.core.base.Pager
import com.tencent.kuikly.core.base.annotation.Page

@Page("Settings")
class SettingsPage : Pager() {
    
    override fun body(): ViewContainer {
        return View {
            attr {
                allCenter()
                size(375f, 667f)
            }
            
            Text {
                attr {
                    text("设置")
                    fontSize(24f)
                    marginTop(50f)
                }
            }
            
            // 设置选项将在这里实现
        }
    }
}