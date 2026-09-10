package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.data.model.DayPriceSummary
import com.example.data.model.DayType
import com.example.data.model.PricePoint
import com.example.ui.components.PriceSummaryCards
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun greeting_screenshot() {
        val testPoints = (0..23).map { h ->
            PricePoint(
                id = "p_$h",
                dateString = "2026-09-10",
                hour = h,
                priceCentPerKwh = 3.5 + (h % 5) * 1.8,
                timestampMillis = 1000L + h * 3600000L,
                dayType = DayType.TODAY
            )
        }
        val summary = DayPriceSummary(
            dayType = DayType.TODAY,
            dateString = "2026-09-10",
            formattedDate = "10.9. (to)",
            prices = testPoints,
            minPrice = testPoints[2],
            maxPrice = testPoints[18],
            avgPrice = 6.45,
            bestChargingWindow = testPoints.subList(1, 4),
            isAvailable = true
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    PriceSummaryCards(summary = summary)
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}

