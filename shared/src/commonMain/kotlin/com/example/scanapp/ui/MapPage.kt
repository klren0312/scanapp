package com.example.scanapp.ui

import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.LocationDao
import com.example.scanapp.models.LocationRecord
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.coroutines.launch
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

@Page("Map")
class MapPage : Pager() {

    private var locationRecords: List<LocationRecord> = emptyList()
    private var totalCount = 0L

    override fun created() {
        super.created()
        loadData()
    }

    override fun body(): ViewContainer<*, *>.() -> Unit = {
        val root = this
        View {
            attr {
                size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                backgroundColor(Color("#F5F5F5"))
                flexDirection(FlexDirection.COLUMN)
                padding(16f)
            }

            TitleText("Locations")
            InfoText("Total records: ${this@MapPage.totalCount}", Color("#333333"))

            Scroller {
                attr {
                    flex(1f)
                    marginTop(8f)
                }
                this@MapPage.locationRecords.forEachIndexed { index, record ->
                    this@MapPage.run { root.LocationItem(index, record) }
                }
                if (this@MapPage.locationRecords.isEmpty()) {
                    InfoText("No location records")
                }
            }
        }
    }

    private fun ViewContainer<*, *>.LocationItem(index: Int, record: LocationRecord) {
        View {
            attr {
                flexDirection(FlexDirection.COLUMN)
                padding(12f)
                marginTop(6f)
                backgroundColor(Color.WHITE)
                borderRadius(8f)
            }
            Text {
                attr {
                    text("#${index + 1} (${record.latitude}, ${record.longitude})")
                    fontSize(15f)
                    color(Color("#333333"))
                }
            }
            View {
                attr {
                    flexDirection(FlexDirection.ROW)
                    justifyContent(FlexJustifyContent.SPACE_BETWEEN)
                    marginTop(6f)
                }
                InfoText("Altitude: ${"%.1f".format(record.altitude)} m")
                InfoText("Accuracy: ${"%.1f".format(record.accuracy)} m")
            }
            InfoText("Timestamp: ${record.timestamp}", Color("#999999"))
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            runCatching {
                val dao = LocationDao(DatabaseFactory.getDatabase())
                totalCount = dao.getCount()
                locationRecords = dao.getAllRecords()
            }.onFailure { it.printStackTrace() }
        }
    }
}
