package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DayPriceSummary
import com.example.data.model.DayType
import com.example.data.model.PriceCategory
import com.example.data.model.PricePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app_name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Nord Pool Sähkö", appName)
    }

    @Test
    fun `price point formatting and categories work correctly`() {
        val pointCheap = PricePoint(
            id = "2026-09-10T04",
            dateString = "2026-09-10",
            hour = 4,
            priceCentPerKwh = 3.12,
            timestampMillis = 1000L,
            dayType = DayType.TODAY
        )

        assertEquals("04:00", pointCheap.displayHour)
        assertEquals("3,12", pointCheap.formattedPrice)
        assertEquals(PriceCategory.VERY_CHEAP, pointCheap.priceCategory)

        val pointExpensive = PricePoint(
            id = "2026-09-10T18",
            dateString = "2026-09-10",
            hour = 18,
            priceCentPerKwh = 26.50,
            timestampMillis = 2000L,
            dayType = DayType.TODAY
        )

        assertEquals("18:00", pointExpensive.displayHour)
        assertEquals("26,50", pointExpensive.formattedPrice)
        assertEquals(PriceCategory.VERY_EXPENSIVE, pointExpensive.priceCategory)
    }

    @Test
    fun `day price summary calculates average and min max`() {
        val prices = listOf(
            PricePoint("1", "2026-09-10", 0, 4.0, 1L, DayType.TODAY),
            PricePoint("2", "2026-09-10", 1, 6.0, 2L, DayType.TODAY),
            PricePoint("3", "2026-09-10", 2, 8.0, 3L, DayType.TODAY)
        )

        val summary = DayPriceSummary(
            dayType = DayType.TODAY,
            dateString = "2026-09-10",
            formattedDate = "10.9. (to)",
            prices = prices,
            minPrice = prices.first(),
            maxPrice = prices.last(),
            avgPrice = 6.0,
            isAvailable = true
        )

        assertEquals("6,00", summary.formattedAvgPrice)
        assertEquals(4.0, summary.minPrice?.priceCentPerKwh ?: 0.0, 0.01)
        assertEquals(8.0, summary.maxPrice?.priceCentPerKwh ?: 0.0, 0.01)
    }
}

