package com.darekbx.geotracker.ui.track

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class SpeedView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    private val chartPaint = Paint().apply {
        isAntiAlias = true
        color = Color.RED
    }

    private val zoomBackgroundPaint = Paint().apply {
        isAntiAlias = true
        color = Color.LTGRAY
    }

    private val guidePaint = Paint().apply {
        isAntiAlias = true
        color = Color.argb(50, 0, 0, 0)
    }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        textSize = 24.0F
    }

    private val backgroundColor = Color.argb(10, 0, 0, 0)
    private val leftPadding = 96F
    private val topPadding = 18F

    private var zoomPosition: Float? = null

    var values: List<Float> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_DOWN -> {
                zoomPosition = event?.x
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                zoomPosition = null
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
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
            var firstPoint = PointF(leftPadding, height - (values.first() * heightRatio))
            val maxPosition = topPadding + (height - (maxValue * heightRatio))
            val avgPosition = topPadding + (height - (avgValue * heightRatio))

            drawGuide(canvas, maxPosition, maxValue)
            drawGuide(canvas, avgPosition, avgValue)
            drawValues(widthRatio, height, heightRatio, canvas, firstPoint)

            drawZoom(widthRatio, heightRatio, canvas)
        }
    }

    private fun drawZoom(
        widthRatio: Float,
        heightRatio: Float,
        canvas: Canvas
    ) {
        zoomPosition?.let {

            val height = getHeight().toFloat()
            val zoomCount = 20
            val position = (it / widthRatio).toInt()

            if (zoomNotAvailable(zoomCount, position)) {
                return
            }

            val zoomValues = mutableListOf<Float>().apply {
                addAll(values.subList(position, position + (zoomCount / 2)))
                addAll(values.subList(position - (zoomCount / 2), position))
            }

            val zoomWidthRatio = zoomCount
            val zoomAreaStart = it - ((zoomCount / 2) * zoomWidthRatio)
            val zoomAreaEnd = it + ((zoomCount / 2) * zoomWidthRatio)

            var zoomFirstPoint = PointF(zoomAreaStart, height - (zoomValues.first() * heightRatio))

            canvas.drawRect(
                zoomAreaStart,
                0F,
                zoomAreaEnd,
                getHeight().toFloat(),
                zoomBackgroundPaint
            )

            zoomValues.forEachIndexed { index, value ->
                if (index == 0) {
                    return@forEachIndexed
                }

                val x = zoomAreaStart + (index * zoomWidthRatio)
                val y = height - (value * heightRatio)

                canvas.drawLine(
                    zoomFirstPoint.x, zoomFirstPoint.y,
                    x, y,
                    chartPaint
                )
                zoomFirstPoint.x = x
                zoomFirstPoint.y = y
            }

        }
    }

    private fun drawValues(
        widthRatio: Float,
        height: Float,
        heightRatio: Float,
        canvas: Canvas,
        firstPoint: PointF
    ) {
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

    private fun zoomNotAvailable(zoomCount: Int, position: Int) =
        values.size < zoomCount * 2
                || position < zoomCount
                || position + zoomCount >= values.size

    private fun drawGuide(canvas: Canvas, guideLinePosition: Float, value: Float) {
        canvas.drawLine(leftPadding, guideLinePosition, getWidth().toFloat(), guideLinePosition, guidePaint)
        canvas.drawText("${(value * 3.6F).toInt()}km\\h", 6F, guideLinePosition + 7F, textPaint)
    }
}
