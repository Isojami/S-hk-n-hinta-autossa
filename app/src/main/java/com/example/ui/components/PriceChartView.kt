package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayPriceSummary
import com.example.data.model.DayType
import com.example.data.model.PricePoint
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.min

@Composable
fun PriceChartView(
    summary: DayPriceSummary,
    inspectedPoint: PricePoint?,
    onInspectPoint: (PricePoint?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!summary.isAvailable || summary.prices.isEmpty()) {
        EmptyChartPlaceholder(message = summary.unavailableMessage ?: "Ei hintatietoja saatavilla", modifier = modifier)
        return
    }

    val prices = summary.prices
    val isToday = summary.dayType == DayType.TODAY
    val nowHelsinki = remember { ZonedDateTime.now(ZoneId.of("Europe/Helsinki")) }
    val currentHour = nowHelsinki.hour

    // Pulsing animation for current live price indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val textMeasurer = rememberTextMeasurer()
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val cyanColor = ElectricCyan
    val greenColor = EmeraldGreen

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .testTag("price_chart_view")
    ) {
        // Chart Header with Active / Inspected Price
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val displayPoint = inspectedPoint
                ?: if (isToday) summary.currentPrice ?: prices.firstOrNull() else prices.firstOrNull()

            Column {
                Text(
                    text = if (inspectedPoint != null) "Valittu tunti: ${inspectedPoint.displayHour}" else if (isToday) "Reaaliaikainen hinta (klo %02d:00)".format(currentHour) else "Päivän hintaprofiili",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = displayPoint?.formattedPrice ?: "-",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "snt / kWh",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            if (isToday) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(cyanColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Canvas Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(prices) {
                        detectTapGestures(
                            onPress = { offset ->
                                val w = size.width
                                val paddingLeft = 40.dp.toPx()
                                val paddingRight = 20.dp.toPx()
                                val plotW = w - paddingLeft - paddingRight
                                val ratio = ((offset.x - paddingLeft) / plotW).coerceIn(0f, 1f)
                                val hour = (ratio * 23f).toInt().coerceIn(0, 23)
                                val pt = prices.firstOrNull { it.hour == hour }
                                onInspectPoint(pt)
                            }
                        )
                    }
                    .pointerInput(prices) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val w = size.width
                                val paddingLeft = 40.dp.toPx()
                                val paddingRight = 20.dp.toPx()
                                val plotW = w - paddingLeft - paddingRight
                                val ratio = ((offset.x - paddingLeft) / plotW).coerceIn(0f, 1f)
                                val hour = (ratio * 23f).toInt().coerceIn(0, 23)
                                val pt = prices.firstOrNull { it.hour == hour }
                                onInspectPoint(pt)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val w = size.width
                                val paddingLeft = 40.dp.toPx()
                                val paddingRight = 20.dp.toPx()
                                val plotW = w - paddingLeft - paddingRight
                                val ratio = ((change.position.x - paddingLeft) / plotW).coerceIn(0f, 1f)
                                val hour = (ratio * 23f).toInt().coerceIn(0, 23)
                                val pt = prices.firstOrNull { it.hour == hour }
                                onInspectPoint(pt)
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height

                val paddingLeft = 42.dp.toPx()
                val paddingRight = 16.dp.toPx()
                val paddingTop = 20.dp.toPx()
                val paddingBottom = 28.dp.toPx()

                val plotWidth = w - paddingLeft - paddingRight
                val plotHeight = h - paddingTop - paddingBottom

                val minPrice = max(0.0, prices.minOf { it.priceCentPerKwh } - 0.5)
                val maxPrice = max(minPrice + 3.0, prices.maxOf { it.priceCentPerKwh } + 1.0)
                val priceRange = maxPrice - minPrice

                // Draw Horizontal Grid lines & Y-axis labels
                val steps = 3
                for (s in 0..steps) {
                    val ratio = s.toFloat() / steps
                    val y = paddingTop + plotHeight * (1f - ratio)
                    val priceVal = minPrice + ratio * priceRange

                    drawLine(
                        color = gridColor,
                        start = Offset(paddingLeft, y),
                        end = Offset(w - paddingRight, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )

                    val textLayout = textMeasurer.measure(
                        text = "%.0f".format(priceVal),
                        style = TextStyle(fontSize = 11.sp, color = axisColor)
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(paddingLeft - textLayout.size.width - 6.dp.toPx(), y - textLayout.size.height / 2f)
                    )
                }

                // X positions for all 24 hours
                val coords = mutableListOf<Offset>()
                for (hour in 0..23) {
                    val pt = prices.firstOrNull { it.hour == hour } ?: continue
                    val x = paddingLeft + (hour / 23f) * plotWidth
                    val normalized = ((pt.priceCentPerKwh - minPrice) / priceRange).toFloat().coerceIn(0f, 1f)
                    val y = paddingTop + plotHeight * (1f - normalized)
                    coords.add(Offset(x, y))
                }

                if (coords.size >= 2) {
                    val strokePath = Path()
                    val fillPath = Path()

                    strokePath.moveTo(coords[0].x, coords[0].y)
                    fillPath.moveTo(coords[0].x, paddingTop + plotHeight)
                    fillPath.lineTo(coords[0].x, coords[0].y)

                    for (i in 0 until coords.size - 1) {
                        val p0 = coords[i]
                        val p1 = coords[i + 1]
                        val midX = (p0.x + p1.x) / 2f
                        strokePath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                        fillPath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                    }

                    fillPath.lineTo(coords.last().x, paddingTop + plotHeight)
                    fillPath.close()

                    // Gradient area fill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                cyanColor.copy(alpha = 0.35f),
                                cyanColor.copy(alpha = 0.03f)
                            ),
                            startY = paddingTop,
                            endY = paddingTop + plotHeight
                        )
                    )

                    // Stroke line
                    drawPath(
                        path = strokePath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(greenColor, cyanColor),
                            startX = paddingLeft,
                            endX = w - paddingRight
                        ),
                        style = Stroke(
                            width = 3.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // Draw X-axis Hour markers (00, 04, 08, 12, 16, 20, 23)
                val keyHours = intArrayOf(0, 4, 8, 12, 16, 20, 23)
                for (kh in keyHours) {
                    val x = paddingLeft + (kh / 23f) * plotWidth
                    val textLayout = textMeasurer.measure(
                        text = "%02d".format(kh),
                        style = TextStyle(fontSize = 11.sp, color = axisColor)
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(x - textLayout.size.width / 2f, h - textLayout.size.height)
                    )
                }

                // Highlight Current Real-Time Hour (if today)
                if (isToday && currentHour in 0..23) {
                    val currCoord = coords.getOrNull(currentHour)
                    if (currCoord != null) {
                        // Vertical indicator line
                        drawLine(
                            color = cyanColor.copy(alpha = 0.7f),
                            start = Offset(currCoord.x, paddingTop),
                            end = Offset(currCoord.x, paddingTop + plotHeight),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )

                        // Pulsing outer halo
                        drawCircle(
                            color = cyanColor.copy(alpha = pulseAlpha),
                            radius = pulseRadius.dp.toPx(),
                            center = currCoord
                        )

                        // Outer ring
                        drawCircle(
                            color = cyanColor,
                            radius = 6.dp.toPx(),
                            center = currCoord,
                            style = Stroke(width = 2.5.dp.toPx())
                        )

                        // Inner dot
                        drawCircle(
                            color = Color.White,
                            radius = 3.5.dp.toPx(),
                            center = currCoord
                        )
                    }
                }

                // Highlight Inspected Hour Point
                if (inspectedPoint != null) {
                    val inspCoord = coords.getOrNull(inspectedPoint.hour)
                    if (inspCoord != null) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.9f),
                            start = Offset(inspCoord.x, paddingTop),
                            end = Offset(inspCoord.x, paddingTop + plotHeight),
                            strokeWidth = 1.5.dp.toPx()
                        )

                        drawCircle(
                            color = Color.White,
                            radius = 5.dp.toPx(),
                            center = inspCoord
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChartPlaceholder(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
