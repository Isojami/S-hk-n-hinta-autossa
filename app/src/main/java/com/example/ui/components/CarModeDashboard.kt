package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.sp
import com.example.data.model.DayPriceSummary
import com.example.data.model.DayType
import com.example.data.model.PricePoint
import com.example.data.repository.ElectricityUiState
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun CarModeDashboard(
    uiState: ElectricityUiState,
    inspectedPoint: PricePoint?,
    onSelectDay: (DayType) -> Unit,
    onInspectPoint: (PricePoint?) -> Unit,
    onRefresh: () -> Unit,
    onExitCarMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSummary = uiState.activeSummary
    val isToday = uiState.selectedDay == DayType.TODAY
    val nowHour = ZonedDateTime.now(ZoneId.of("Europe/Helsinki")).hour

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
            .testTag("car_mode_dashboard")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Automotive Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AUTOTILA • NORD POOL",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = ElectricCyan
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SlateSurfaceVariant)
                            .testTag("car_mode_refresh_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Päivitä",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onExitCarMode,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SlateSurfaceVariant)
                            .testTag("car_mode_exit_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Poistu autotilasta",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Day Tabs: [Eilinen] [Tänään] [Huominen]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DayType.values().forEach { day ->
                    val isSelected = uiState.selectedDay == day
                    Button(
                        onClick = { onSelectDay(day) },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("car_tab_${day.name.lowercase()}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) ElectricCyan else SlateSurface,
                            contentColor = if (isSelected) SlateBackground else Color.White
                        ),
                        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, SlateBorder) else null
                    ) {
                        Text(
                            text = day.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hero Live Price Gauge / Big Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = SlateSurface,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isToday) ElectricCyan else SlateBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isToday) "SÄHKÖNHINTA NYT (klo %02d:00)".format(nowHour) else "${uiState.selectedDay.title.uppercase()} • KESKIARVO",
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                            color = ElectricCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            val priceVal = if (isToday) {
                                currentSummary.currentPrice?.formattedPrice ?: currentSummary.formattedAvgPrice
                            } else {
                                currentSummary.formattedAvgPrice
                            }
                            Text(
                                text = priceVal,
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "snt / kWh",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ElectricCyan,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }

                    // Status pill
                    val statusText = when {
                        !currentSummary.isAvailable -> "Ei tietoja"
                        isToday && (currentSummary.currentPrice?.priceCentPerKwh ?: 0.0) < 5.0 -> "ERITTÄIN HALPA"
                        isToday && (currentSummary.currentPrice?.priceCentPerKwh ?: 0.0) < 10.0 -> "EDULLINEN"
                        isToday && (currentSummary.currentPrice?.priceCentPerKwh ?: 0.0) < 15.0 -> "KOHTUULLINEN"
                        else -> "KORKEA"
                    }
                    val statusColor = when {
                        !currentSummary.isAvailable -> Color.Gray
                        isToday && (currentSummary.currentPrice?.priceCentPerKwh ?: 0.0) < 10.0 -> EmeraldGreen
                        isToday && (currentSummary.currentPrice?.priceCentPerKwh ?: 0.0) < 15.0 -> ElectricCyan
                        else -> AlertRed
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusColor.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real-Time Automotive Spot Price Chart
            PriceChartView(
                summary = currentSummary,
                inspectedPoint = inspectedPoint,
                onInspectPoint = onInspectPoint,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Driving Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = SlateSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "HALVIN TUNTI",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${currentSummary.minPrice?.formattedPrice ?: "-"} snt",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "klo ${currentSummary.minPrice?.displayHour ?: "-"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = SlateSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "KALLEIN TUNTI",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AlertRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${currentSummary.maxPrice?.formattedPrice ?: "-"} snt",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "klo ${currentSummary.maxPrice?.displayHour ?: "-"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // EV Charging helper in Car Mode
            if (currentSummary.bestChargingWindow.isNotEmpty()) {
                val startHour = currentSummary.bestChargingWindow.first().displayHour
                val endHour = "%02d:00".format((currentSummary.bestChargingWindow.last().hour + 1) % 24)
                val windowAvg = currentSummary.bestChargingWindow.map { it.priceCentPerKwh }.average()
                val windowAvgFormatted = "%.2f".format(windowAvg).replace('.', ',')

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = SlateSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(EmeraldGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricCar,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "PARAS LATAUSAIKA AUTOLLE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldGreen
                            )
                            Text(
                                text = "$startHour – $endHour",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "3 tunnin keskihinta vain $windowAvgFormatted snt/kWh",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }
        }
    }
}
