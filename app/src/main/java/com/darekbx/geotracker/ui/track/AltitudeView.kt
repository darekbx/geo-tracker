package com.darekbx.geotracker.ui.track

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View

class AltitudeView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    private val chartPaint = Paint().apply {
        isAntiAlias = true
        color = Color.GREEN
    }

    private val guidePaint = Paint().apply {
        isAntiAlias = false
        strokeWidth = 1.0F
        color = Color.argb(50, 255, 255, 255)
    }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textSize = 24.0F
    }

    private val leftPadding = 96F
    private val topPadding = 18F

    var values: List<Float> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (values.isEmpty() || canvas == null) {
            return
        }

        val height = height - topPadding
        val width = width - leftPadding

        val count = values.count()
        val maxValue = (values.max() ?: 1.0F)
        val minValue = (values.min() ?: 1.0F)
        val widthRatio = width / count.toFloat()
        val heightRatio = height / (maxValue - minValue)
        val firstPoint = PointF(leftPadding, height - ((values.first() - minValue) * heightRatio))
        val maxPosition = topPadding + (height - ((maxValue - minValue) * heightRatio))

        drawGuide(canvas, maxPosition, maxValue)
        drawGuide(canvas, height, minValue)
        drawValues(widthRatio, height, heightRatio, canvas, firstPoint, minValue)
    }

    private fun drawValues(
        widthRatio: Float,
        height: Float,
        heightRatio: Float,
        canvas: Canvas,
        firstPoint: PointF,
        minValue: Float
    ) {
        values.forEachIndexed { index, value ->
            if (index == 0) {
                return@forEachIndexed
            }

            val x = leftPadding + (index * widthRatio)
            val y = height - ((value - minValue) * heightRatio)

            canvas.drawLine(
                firstPoint.x, topPadding + firstPoint.y,
                x, topPadding + y,
                chartPaint
            )
            firstPoint.x = x
            firstPoint.y = y
        }
    }

    private fun drawGuide(canvas: Canvas, guideLinePosition: Float, value: Float) {
        canvas.drawLine(leftPadding, guideLinePosition, width.toFloat(), guideLinePosition, guidePaint)
        canvas.drawText("${value.toInt()}m", 6F, guideLinePosition + 7F, textPaint)
    }
}
