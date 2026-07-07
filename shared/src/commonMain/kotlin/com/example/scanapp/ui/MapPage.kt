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
                backgroundColor(MdcTheme.Colors.background)
                flexDirection(FlexDirection.COLUMN)
                padding(MdcTheme.Spacing.md)
            }

            MdcTitle("Locations")
            MdcBodyText("Total records: ${this@MapPage.totalCount}", MdcTheme.Colors.onSurfaceVariant)

            Scroller {
                attr {
                    flex(1f)
                    marginTop(MdcTheme.Spacing.sm)
                }
                this@MapPage.locationRecords.forEachIndexed { index, record ->
                    this@MapPage.run { root.MdcLocationCard(index, record) }
                }
                if (this@MapPage.locationRecords.isEmpty()) {
                    MdcBodyText("No location records", MdcTheme.Colors.onSurfaceVariant)
                }
            }
        }
    }

    private fun ViewContainer<*, *>.MdcLocationCard(index: Int, record: LocationRecord) {
        MdcCard {
            Text {
                attr {
                    text("#${index + 1}  (${record.latitude}, ${record.longitude})")
                    fontSize(MdcTheme.Typography.bodyLarge)
                    fontWeightMedium()
                    color(MdcTheme.Colors.onSurface)
                }
            }
            View {
                attr {
                    flexDirection(FlexDirection.ROW)
                    justifyContent(FlexJustifyContent.SPACE_BETWEEN)
                    marginTop(MdcTheme.Spacing.sm)
                }
                MdcCaption("Altitude: ${"%.1f".format(record.altitude)} m")
                MdcCaption("Accuracy: ${"%.1f".format(record.accuracy)} m")
            }
            MdcCaption("Timestamp: ${record.timestamp}")
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
