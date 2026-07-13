package com.example.scanapp.ui

import com.example.scanapp.logging.CrashLogger
import com.example.scanapp.logging.LogEntry
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.View

@Page("Logs")
class LogsPage : Pager() {

    private var logs by observableList<LogEntry>()

    override fun created() {
        super.created()
        reload()
    }

    private fun reload() {
        logs.clear()
        logs.addAll(CrashLogger.all())
    }

    override fun body(): ViewContainer<*, *>.() -> Unit = {
        View {
            attr {
                size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                backgroundColor(MdcTheme.Colors.background)
                flexDirection(FlexDirection.COLUMN)
                padding(MdcTheme.Spacing.md)
            }

            MdcTopBar({ "Crash Logs" }) { this@LogsPage.closePage() }

            View {
                attr {
                    flexDirection(FlexDirection.ROW)
                    justifyContent(FlexJustifyContent.SPACE_BETWEEN)
                    marginTop(MdcTheme.Spacing.xs)
                }
                MdcTextButton("Refresh") { this@LogsPage.reload() }
                MdcTextButton("Clear") { CrashLogger.clear(); this@LogsPage.reload() }
            }

            Scroller {
                attr {
                    flex(1f)
                    marginTop(MdcTheme.Spacing.sm)
                }
                vfor({ this@LogsPage.logs }) { entry ->
                    MdcCard {
                        View {
                            attr {
                                flexDirection(FlexDirection.ROW)
                                marginBottom(MdcTheme.Spacing.xs)
                            }
                            MdcCaption("#${entry.id}  ${entry.tag}", MdcTheme.Colors.error)
                        }
                        MdcCaption(entry.message)
                        if (entry.stack.isNotEmpty()) {
                            MdcCaption(entry.stack)
                        }
                    }
                }
                vif({ this@LogsPage.logs.isEmpty() }) {
                    MdcBodyText("No logs recorded", MdcTheme.Colors.onSurfaceVariant)
                }
            }
        }
    }

    private fun closePage() {
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
    }
}
