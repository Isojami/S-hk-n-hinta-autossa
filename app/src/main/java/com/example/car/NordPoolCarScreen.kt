package com.example.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.data.model.DayType
import com.example.data.repository.ElectricityRepository
import com.example.data.repository.ElectricityUiState
import kotlinx.coroutines.launch

class NordPoolCarScreen(
    carContext: CarContext,
    private val repository: ElectricityRepository = ElectricityRepository.getInstance(carContext)
) : Screen(carContext) {

    private var selectedDay: DayType = DayType.TODAY

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                lifecycleScope.launch {
                    repository.uiState.collect {
                        invalidate()
                    }
                }
                lifecycleScope.launch {
                    repository.refreshPrices(false)
                }
            }
        })
    }

    override fun onGetTemplate(): Template {
        val state: ElectricityUiState = repository.uiState.value
        val summary = when (selectedDay) {
            DayType.YESTERDAY -> state.yesterday
            DayType.TODAY -> state.today
            DayType.TOMORROW -> state.tomorrow
        }

        val paneBuilder = Pane.Builder()

        // 1. Render the real-time spot price chart into a high-contrast bitmap for the car display
        val chartBitmap = CarChartRenderer.renderChartBitmap(summary, width = 720, height = 280)
        val chartCarIcon = CarIcon.Builder(IconCompat.createWithBitmap(chartBitmap)).build()

        // Row 1: Chart preview image
        val chartRow = Row.Builder()
            .setTitle("${selectedDay.title} (${summary.formattedDate})")
            .setImage(chartCarIcon, Row.IMAGE_TYPE_LARGE)
            .build()
        paneBuilder.addRow(chartRow)

        // Row 2: Price summary row (Current price or average)
        val priceRowBuilder = Row.Builder()
        if (selectedDay == DayType.TODAY && summary.currentPrice != null) {
            priceRowBuilder
                .setTitle("Nyt: ${summary.currentPrice.formattedPrice} snt/kWh")
                .addText("Ka: ${summary.formattedAvgPrice} snt | Min: ${summary.minPrice?.formattedPrice ?: "-"} snt | Max: ${summary.maxPrice?.formattedPrice ?: "-"} snt")
        } else if (summary.isAvailable) {
            priceRowBuilder
                .setTitle("Keskiarvo: ${summary.formattedAvgPrice} snt/kWh")
                .addText("Halvin: ${summary.minPrice?.formattedPrice ?: "-"} snt (${summary.minPrice?.displayHour ?: ""}) | Kallein: ${summary.maxPrice?.formattedPrice ?: "-"} snt (${summary.maxPrice?.displayHour ?: ""})")
        } else {
            priceRowBuilder
                .setTitle(selectedDay.title)
                .addText(summary.unavailableMessage ?: "Ei hintatietoja saatavilla")
        }
        paneBuilder.addRow(priceRowBuilder.build())

        // Row 3: Best charging hours for electric vehicles
        if (summary.bestChargingWindow.isNotEmpty()) {
            val startHour = summary.bestChargingWindow.first().displayHour
            val endHour = "%02d:00".format((summary.bestChargingWindow.last().hour + 1) % 24)
            val windowAvg = summary.bestChargingWindow.map { it.priceCentPerKwh }.average()
            val windowAvgFormatted = "%.2f".format(windowAvg).replace('.', ',')

            val chargingRow = Row.Builder()
                .setTitle("Paras latausaika: $startHour – $endHour")
                .addText("Edullisin 3h jakso autolle (ka. $windowAvgFormatted snt/kWh)")
                .build()
            paneBuilder.addRow(chargingRow)
        }

        // Action strip to switch between Eilinen, Tänään, Huominen + Refresh
        val actionStripBuilder = ActionStrip.Builder()

        // Switch to Yesterday
        val yesterdayAction = Action.Builder()
            .setTitle(if (selectedDay == DayType.YESTERDAY) "• Eilinen" else "Eilinen")
            .setOnClickListener {
                if (selectedDay != DayType.YESTERDAY) {
                    selectedDay = DayType.YESTERDAY
                    invalidate()
                }
            }
            .build()
        actionStripBuilder.addAction(yesterdayAction)

        // Switch to Today
        val todayAction = Action.Builder()
            .setTitle(if (selectedDay == DayType.TODAY) "• Tänään" else "Tänään")
            .setOnClickListener {
                if (selectedDay != DayType.TODAY) {
                    selectedDay = DayType.TODAY
                    invalidate()
                }
            }
            .build()
        actionStripBuilder.addAction(todayAction)

        // Switch to Tomorrow
        val tomorrowAction = Action.Builder()
            .setTitle(if (selectedDay == DayType.TOMORROW) "• Huominen" else "Huominen")
            .setOnClickListener {
                if (selectedDay != DayType.TOMORROW) {
                    selectedDay = DayType.TOMORROW
                    invalidate()
                }
            }
            .build()
        actionStripBuilder.addAction(tomorrowAction)

        // Refresh action
        val refreshAction = Action.Builder()
            .setTitle("Päivitä")
            .setOnClickListener {
                lifecycleScope.launch {
                    repository.refreshPrices(true)
                }
            }
            .build()
        actionStripBuilder.addAction(refreshAction)

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle("Nord Pool Sähkö")
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(actionStripBuilder.build())
            .build()
    }
}
