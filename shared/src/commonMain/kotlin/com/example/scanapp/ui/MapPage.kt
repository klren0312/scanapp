package com.example.scanapp.ui

import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.LocationDao
import com.example.scanapp.models.LocationRecord
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.coroutines.delay
import com.tencent.kuikly.core.coroutines.launch
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

@Page("Map")
class MapPage : Pager() {

    private var locationRecords by observable(emptyList<LocationRecord>())
    private var totalCount by observable(0L)
    private var drawerOpen by observable(false)

    override fun created() {
        super.created()
        refreshData()
        lifecycleScope.launch {
            while (true) {
                delay(3000)
                refreshData()
            }
        }
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

            MdcMenuTopBar("Locations") { this@MapPage.drawerOpen = true }
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

            MdcNavigationDrawerHost(
                isOpen = { this@MapPage.drawerOpen },
                currentPage = { "Map" },
                onClose = { this@MapPage.drawerOpen = false },
                onNavigate = { this@MapPage.navigateTo(it) }
            )
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
                MdcCaption("Altitude: ${this@MapPage.formatOneDecimal(record.altitude)} m")
                MdcCaption("Accuracy: ${this@MapPage.formatOneDecimal(record.accuracy.toDouble())} m")
            }
            MdcCaption("Timestamp: ${record.timestamp}")
        }
    }

    private fun refreshData() {
        lifecycleScope.launch {
            runCatching {
                val dao = LocationDao(DatabaseFactory.getDatabase())
                totalCount = dao.getCount()
                locationRecords = dao.getAllRecords()
            }.onFailure { it.printStackTrace() }
        }
    }

    private fun formatOneDecimal(value: Double): String {
        val tenths = kotlin.math.round(value * 10.0).toLong()
        val sign = if (tenths < 0) "-" else ""
        val absolute = kotlin.math.abs(tenths)
        return "$sign${absolute / 10}.${absolute % 10}"
    }

    private fun navigateTo(pageName: String) {
        drawerOpen = false
        if (pageName == "Map") return
        if (pageName == "Scanner") {
            closePage()
            return
        }
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).openPage(pageName = pageName)
        closePage()
    }

    private fun closePage() {
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
    }
}
