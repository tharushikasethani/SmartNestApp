package com.example.smartnest.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.smartnest.R

class UsageBarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }
    
    var data: List<Pair<String, Float>> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    private val barColor = ContextCompat.getColor(context, R.color.orange_mid)
    private val textColor = ContextCompat.getColor(context, R.color.text_primary)
    private val secondaryTextColor = ContextCompat.getColor(context, R.color.text_secondary)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) {
            textPaint.color = secondaryTextColor
            canvas.drawText("No data", width / 2f, height / 2f, textPaint)
            return
        }

        val width = width.toFloat()
        val height = height.toFloat()
        val paddingHorizontal = 60f
        val paddingTop = 40f
        val paddingBottom = 80f
        
        val chartHeight = height - paddingTop - paddingBottom
        val chartWidth = width - paddingHorizontal * 2

        val maxVal = data.maxOf { it.second }.coerceAtLeast(1f)
        val barCount = data.size
        val barWidth = (chartWidth / barCount) * 0.5f
        val spacing = (chartWidth / barCount) * 0.5f

        data.forEachIndexed { index, pair ->
            val barHeight = (pair.second / maxVal) * chartHeight
            val left = paddingHorizontal + index * (barWidth + spacing) + spacing / 2
            val top = height - paddingBottom - barHeight
            val right = left + barWidth
            val bottom = height - paddingBottom

            // Draw bar
            paint.color = barColor
            canvas.drawRoundRect(RectF(left, top, right, bottom), 12f, 12f, paint)

            // Draw label (device name)
            textPaint.color = textColor
            val label = if (pair.first.length > 8) pair.first.substring(0, 6) + ".." else pair.first
            canvas.drawText(label, (left + right) / 2, height - paddingBottom / 2 + 10f, textPaint)
            
            // Draw value (hours)
            textPaint.color = secondaryTextColor
            textPaint.textSize = 24f
            val valStr = String.format("%.1fh", pair.second)
            canvas.drawText(valStr, (left + right) / 2, top - 15f, textPaint)
            textPaint.textSize = 28f
        }
    }
}
