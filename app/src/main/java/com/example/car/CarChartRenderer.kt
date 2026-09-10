package com.example.car

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.example.data.model.DayPriceSummary
import java.time.ZonedDateTime
import java.time.ZoneId
import kotlin.math.max

object CarChartRenderer {

    /**
     * Renders a crisp, high-contrast 24-hour spot price chart optimized for car head units.
     */
    fun renderChartBitmap(
        summary: DayPriceSummary,
        width: Int = 720,
        height: Int = 300
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background: Deep automotive slate/navy
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
        }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 16f, 16f, bgPaint)

        val prices = summary.prices
        if (prices.isEmpty()) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#94A3B8")
                textSize = 28f
                textAlign = Paint.Align.CENTER
            }
            val msg = summary.unavailableMessage ?: "Ei hintatietoja saatavilla"
            canvas.drawText(msg, width / 2f, height / 2f, textPaint)
            return bitmap
        }

        val paddingLeft = 50f
        val paddingRight = 40f
        val paddingTop = 45f
        val paddingBottom = 55f

        val plotWidth = width - paddingLeft - paddingRight
        val plotHeight = height - paddingTop - paddingBottom

        val minVal = max(0.0, prices.minOf { it.priceCentPerKwh } - 1.0)
        val maxVal = max(minVal + 4.0, prices.maxOf { it.priceCentPerKwh } + 2.0)
        val range = maxVal - minVal

        // Grid lines & labels
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 20f
            textAlign = Paint.Align.RIGHT
        }

        // Draw 3 horizontal grid lines (min, mid, max)
        for (step in 0..2) {
            val ratio = step / 2f
            val y = paddingTop + plotHeight * (1f - ratio)
            val valAtY = minVal + ratio * range
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint)
            canvas.drawText("%.0f".format(valAtY), paddingLeft - 8f, y + 6f, labelPaint)
        }

        // Calculate points (24 hours: 0..23)
        val points = mutableListOf<Pair<Float, Float>>()
        for (i in 0 until 24) {
            val pt = prices.firstOrNull { it.hour == i } ?: continue
            val x = paddingLeft + (i / 23f) * plotWidth
            val y = (paddingTop + plotHeight * (1f - ((pt.priceCentPerKwh - minVal) / range).toFloat()))
                .coerceIn(paddingTop, paddingTop + plotHeight)
            points.add(Pair(x, y))
        }

        if (points.size >= 2) {
            // Path for stroke and fill
            val strokePath = Path()
            val fillPath = Path()

            strokePath.moveTo(points[0].first, points[0].second)
            fillPath.moveTo(points[0].first, paddingTop + plotHeight)
            fillPath.lineTo(points[0].first, points[0].second)

            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val midX = (p0.first + p1.first) / 2f
                strokePath.cubicTo(midX, p0.second, midX, p1.second, p1.first, p1.second)
                fillPath.cubicTo(midX, p0.second, midX, p1.second, p1.first, p1.second)
            }

            fillPath.lineTo(points.last().first, paddingTop + plotHeight)
            fillPath.close()

            // Draw Area Fill with gradient
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                shader = LinearGradient(
                    0f, paddingTop, 0f, paddingTop + plotHeight,
                    intArrayOf(Color.parseColor("#4400E5FF"), Color.parseColor("#0500E5FF")),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawPath(fillPath, fillPaint)

            // Draw Stroke
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 4.5f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                shader = LinearGradient(
                    paddingLeft, 0f, width - paddingRight, 0f,
                    Color.parseColor("#00E5FF"), Color.parseColor("#38BDF8"),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawPath(strokePath, strokePaint)
        }

        // Current hour highlight (if today)
        val nowHelsinki = ZonedDateTime.now(ZoneId.of("Europe/Helsinki"))
        val currentHour = nowHelsinki.hour
        val isToday = summary.dayType == com.example.data.model.DayType.TODAY

        val axisTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }

        // X-axis hours (00, 04, 08, 12, 16, 20, 23)
        val keyHours = intArrayOf(0, 4, 8, 12, 16, 20, 23)
        for (h in keyHours) {
            val x = paddingLeft + (h / 23f) * plotWidth
            val text = "%02d".format(h)
            canvas.drawText(text, x, height - 15f, axisTextPaint)
        }

        // Draw indicator for current hour if applicable
        if (isToday && currentHour in 0..23) {
            val currPt = points.firstOrNull { (prices.getOrNull(points.indexOf(it))?.hour ?: -1) == currentHour }
                ?: points.getOrNull(currentHour)

            if (currPt != null) {
                val cx = currPt.first
                val cy = currPt.second

                // Vertical dash line
                val indicatorLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#00E5FF")
                    strokeWidth = 2.5f
                }
                canvas.drawLine(cx, paddingTop, cx, paddingTop + plotHeight, indicatorLinePaint)

                // Pulse ring
                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#5500E5FF")
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(cx, cy, 14f, ringPaint)

                // Center solid dot
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#FFFFFF")
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(cx, cy, 7f, dotPaint)

                // Live price badge above dot
                val currPrice = summary.prices.firstOrNull { it.hour == currentHour }?.priceCentPerKwh
                if (currPrice != null) {
                    val badgeText = "%.1f snt".format(currPrice).replace('.', ',')
                    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#00E5FF")
                        textSize = 22f
                        isFakeBoldText = true
                        textAlign = Paint.Align.CENTER
                    }
                    val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#1E293B")
                        style = Paint.Style.FILL
                    }
                    val textW = badgePaint.measureText(badgeText)
                    val bLeft = (cx - textW / 2 - 10f).coerceIn(paddingLeft, width - paddingRight - textW - 20f)
                    val bTop = (cy - 38f).coerceAtLeast(10f)
                    canvas.drawRoundRect(RectF(bLeft, bTop, bLeft + textW + 20f, bTop + 30f), 8f, 8f, badgeBgPaint)
                    canvas.drawText(badgeText, bLeft + (textW + 20f) / 2f, bTop + 22f, badgePaint)
                }
            }
        }

        return bitmap
    }
}
