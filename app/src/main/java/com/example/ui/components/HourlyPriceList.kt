package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DayPriceSummary
import com.example.data.model.DayType
import com.example.data.model.PriceCategory
import com.example.data.model.PricePoint
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrightYellow
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.max

@Composable
fun HourlyPriceList(
    summary: DayPriceSummary,
    inspectedPoint: PricePoint?,
    onSelectPoint: (PricePoint) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!summary.isAvailable || summary.prices.isEmpty()) return

    val prices = summary.prices
    val maxPrice = max(5.0, prices.maxOf { it.priceCentPerKwh })
    val isToday = summary.dayType == DayType.TODAY
    val nowHour = ZonedDateTime.now(ZoneId.of("Europe/Helsinki")).hour
    val chargingHours = summary.bestChargingWindow.map { it.hour }.toSet()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hourly_price_list")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tuntihinnat (24h)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Napauta tuntia tarkastellaksesi",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            prices.forEach { point ->
                val isCurrentHour = isToday && point.hour == nowHour
                val isInspected = inspectedPoint?.hour == point.hour
                val isCharging = chargingHours.contains(point.hour)
                val relativeRatio = (point.priceCentPerKwh / maxPrice).toFloat().coerceIn(0.05f, 1f)

                HourlyItemCard(
                    point = point,
                    isCurrentHour = isCurrentHour,
                    isInspected = isInspected,
                    isCharging = isCharging,
                    relativeRatio = relativeRatio,
                    onClick = { onSelectPoint(point) }
                )
            }
        }
    }
}

@Composable
private fun HourlyItemCard(
    point: PricePoint,
    isCurrentHour: Boolean,
    isInspected: Boolean,
    isCharging: Boolean,
    relativeRatio: Float,
    onClick: () -> Unit
) {
    val categoryColor = when (point.priceCategory) {
        PriceCategory.VERY_CHEAP -> EmeraldGreen
        PriceCategory.CHEAP -> ElectricCyan
        PriceCategory.NORMAL -> BrightYellow
        PriceCategory.EXPENSIVE -> AlertAmber
        PriceCategory.VERY_EXPENSIVE -> AlertRed
    }

    val borderColor = when {
        isInspected -> Color.White
        isCurrentHour -> ElectricCyan
        else -> MaterialTheme.colorScheme.outline
    }

    val borderWidth = if (isInspected || isCurrentHour) 1.5.dp else 1.dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("hour_row_${point.hour}"),
        shape = RoundedCornerShape(12.dp),
        color = if (isInspected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hour title
            Text(
                text = "%02d:00 - %02d:00".format(point.hour, (point.hour + 1) % 24),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isCurrentHour) FontWeight.Bold else FontWeight.Medium),
                color = if (isCurrentHour) ElectricCyan else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(110.dp)
            )

            // Tags
            if (isCurrentHour) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ElectricCyan.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "NYT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ElectricCyan
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            } else if (isCharging) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(EmeraldGreen.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LATAUS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldGreen
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Visual bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(relativeRatio)
                        .clip(RoundedCornerShape(4.dp))
                        .background(categoryColor)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Price in snt/kWh
            Text(
                text = "${point.formattedPrice} snt",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
