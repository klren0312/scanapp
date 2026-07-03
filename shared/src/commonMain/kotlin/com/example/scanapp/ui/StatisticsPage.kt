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

@Page("Statistics")
class StatisticsPage : Pager() {
    
    override fun body(): ViewContainer {
        return View {
            attr {
                allCenter()
                size(375f, 667f)
            }
            
            Text {
                attr {
                    text("统计信息")
                    fontSize(24f)
                    marginTop(50f)
                }
            }
            
            // 统计图表将在这里实现
        }
    }
}