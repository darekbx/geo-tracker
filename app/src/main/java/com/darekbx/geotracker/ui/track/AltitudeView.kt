package com.darekbx.geotracker.ui.track

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View

class AltitudeView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    val chartPaint = Paint().apply {
        isAntiAlias = true
        color = Color.RED
    }

    val guidePaint = Paint().apply {
        isAntiAlias = true
        color = Color.argb(50, 0, 0, 0)
    }

    val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        textSize = 24.0F
    }

    val backgroundColor = Color.argb(10, 0, 0, 0)
    val leftPadding = 96F
    val topPadding = 18F

    var values: List<Float> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let { canvas ->

            canvas.drawColor(backgroundColor)

            if (values.isEmpty()) {
                return
            }

            val height = getHeight() - topPadding
            val width = getWidth() - leftPadding

            val count = values.count()
            val maxValue = (values.max() ?: 1.0F)
            val minValue = (values.min() ?: 1.0F)
            val widthRatio = width / count.toFloat()
            val heightRatio = height / (maxValue - minValue)
            var firstPoint = PointF(leftPadding, height - ((values.first() - minValue) * heightRatio))
            val maxPosition = topPadding + (height - ((maxValue - minValue) * heightRatio))

            drawGuide(canvas, maxPosition, maxValue)
            drawGuide(canvas, height, minValue)
            drawValues(widthRatio, height, heightRatio, canvas, firstPoint, minValue)
        }
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
        canvas.drawLine(leftPadding, guideLinePosition, getWidth().toFloat(), guideLinePosition, guidePaint)
        canvas.drawText("${value.toInt()}m", 6F, guideLinePosition + 7F, textPaint)
    }
}
