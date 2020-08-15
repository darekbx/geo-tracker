package com.darekbx.geotracker.ui.track

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View

class SpeedView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

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
            val avgValue = values.average().toFloat()
            val widthRatio = width / count.toFloat()
            val heightRatio = height / maxValue

            var firstPoint = PointF(leftPadding, height - values.first())

            val maxPosition = topPadding + (height - (maxValue * heightRatio))
            val avgPosition = topPadding + (height - (avgValue * heightRatio))
            drawGuide(canvas, maxPosition, maxValue)
            drawGuide(canvas, avgPosition, avgValue)

            values.forEachIndexed { index, value ->
                if (index == 0) {
                    return@forEachIndexed
                }

                val x = leftPadding + (index * widthRatio)
                val y = height - (value * heightRatio)

                canvas.drawLine(
                    firstPoint.x, topPadding + firstPoint.y,
                    x, topPadding + y,
                    chartPaint
                )
                firstPoint.x = x
                firstPoint.y = y
            }
        }
    }

    private fun drawGuide(canvas: Canvas, guideLinepPosition: Float, value: Float) {
        canvas.drawLine(leftPadding, guideLinepPosition, getWidth().toFloat(), guideLinepPosition, guidePaint)
        canvas.drawText("${(value * 3.6F).toInt()}km\\h", 6F, guideLinepPosition + 7F, textPaint)
    }
}
