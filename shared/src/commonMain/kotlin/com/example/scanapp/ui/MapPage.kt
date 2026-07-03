package com.example.scanapp.ui

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.size
import com.tencent.kuikly.core.layout.width
import com.tencent.kuikly.core.layout.height
import com.tencent.kuikly.core.layout.flexDirection
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.justifyContent
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.layout.alignItems
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.alignSelf
import com.tencent.kuikly.core.layout.padding
import com.tencent.kuikly.core.layout.marginTop
import com.tencent.kuikly.core.layout.marginBottom
import com.tencent.kuikly.core.layout.marginRight
import com.tencent.kuikly.core.layout.flex
import com.tencent.kuikly.core.layout.backgroundColor
import com.tencent.kuikly.core.layout.borderRadius
import com.tencent.kuikly.core.layout.color
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.reactive.variable
import com.tencent.kuikly.core.base.Page
import com.tencent.kuikly.core.base.Pager
import com.tencent.kuikly.core.base.annotation.Page
import com.tencent.kuikly.core.base.Color
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.LocationDao
import com.example.scanapp.models.LocationRecord

@Page("Map")
class MapPage : Pager() {

    private val locationRecords = variable<List<LocationRecord>>(emptyList())
    private val totalCount = variable(0L)

    override fun created() {
        super.created()
        loadData()
    }

    override fun body(): ViewContainer {
        return View {
            attr {
                size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                backgroundColor(Color.parse("#F5F5F5"))
                flexDirection(FlexDirection.COLUMN)
                padding(16f)
            }

            Text {
                attr {
                    text("位置记录")
                    fontSize(24f)
                    marginTop(40f)
                    alignSelf(FlexAlign.CENTER)
                    color(Color.parse("#333333"))
                }
            }

            View {
                attr {
                    padding(12f)
                    marginTop(12f)
                    backgroundColor(Color.WHITE)
                    borderRadius(8f)
                    width(pagerData.pageViewWidth - 32f)
                    alignItems(FlexAlign.CENTER)
                }
                Text {
                    attr {
                        text("共 $totalCount 条记录")
                        fontSize(16f)
                        color(Color.parse("#666666"))
                    }
                }
            }

            Scroller {
                attr {
                    flex(1f)
                    width(pagerData.pageViewWidth - 32f)
                    marginTop(8f)
                }

                locationRecords.value.forEachIndexed { index, record ->
                    LocationItemView(index = index, record = record)
                }

                if (locationRecords.value.isEmpty()) {
                    Text {
                        attr {
                            text("暂无位置记录")
                            fontSize(14f)
                            color(Color.parse("#999999"))
                            alignSelf(FlexAlign.CENTER)
                            marginTop(60f)
                        }
                    }
                }
            }
        }
    }

    private fun ViewContainer.LocationItemView(index: Int, record: LocationRecord) {
        View {
            attr {
                flexDirection(FlexDirection.COLUMN)
                padding(12f)
                marginTop(6f)
                backgroundColor(Color.WHITE)
                borderRadius(8f)
                width(pagerData.pageViewWidth - 32f)
            }

            Text {
                attr {
                    text("#${index + 1}  (${record.latitude}, ${record.longitude})")
                    fontSize(15f)
                    color(Color.parse("#333333"))
                }
            }

            View {
                attr {
                    flexDirection(FlexDirection.ROW)
                    justifyContent(FlexJustifyContent.SPACE_BETWEEN)
                    marginTop(6f)
                }
                Text {
                    attr {
                        text("高度: ${"%.1f".format(record.altitude)}m")
                        fontSize(13f)
                        color(Color.parse("#999999"))
                    }
                }
                Text {
                    attr {
                        text("精度: ${"%.1f".format(record.accuracy)}m")
                        fontSize(13f)
                        color(Color.parse("#999999"))
                    }
                }
            }

            Text {
                attr {
                    text(formatTimestamp(record.timestamp))
                    fontSize(12f)
                    marginTop(4f)
                    color(Color.parse("#BBBBBB"))
                }
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val seconds = timestamp / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        val year = 2026
        val month = 7
        val day = 3

        val h = (hours % 24).toInt()
        val m = (minutes % 60).toInt()
        val s = (seconds % 60).toInt()

        return "${year}-${"%02d".format(month)}-${"%02d".format(day)} ${"%02d".format(h)}:${"%02d".format(m)}:${"%02d".format(s)}"
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val db = DatabaseFactory.getDatabase()
                val dao = LocationDao(db)
                totalCount.value = dao.getCount()
                locationRecords.value = dao.getAllRecords()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
