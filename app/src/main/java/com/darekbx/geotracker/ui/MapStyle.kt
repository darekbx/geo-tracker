package com.darekbx.geotracker.ui

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import com.darekbx.geotracker.utils.AppPreferences
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay
import javax.inject.Inject

class MapStyle @Inject constructor(
    private val appPreferences: AppPreferences
) {

    enum class MapStyleEnum(val styleId: Int) {
        CLASSIC(0),
        DARK(1),
        CUSTOM(2);
    }

    fun applyMapStyle(mapView: MapView) {
        val mapStyleEnum = MapStyleEnum.values()
            .first { it.styleId == appPreferences.mapStyle }
        when (mapStyleEnum) {
            MapStyleEnum.CLASSIC -> applyClassic(mapView)
            MapStyleEnum.DARK -> applyDark(mapView)
            MapStyleEnum.CUSTOM -> applyCustom(mapView)
        }
    }

    private fun applyClassic(mapView: MapView) {
        mapView.getOverlayManager().getTilesOverlay().setColorFilter(null)
    }

    private fun applyDark(mapView: MapView) {
        mapView.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS)
    }

    private fun applyCustom(mapView: MapView) {
        val inverseMatrix = ColorMatrix(
            floatArrayOf(
                -1.0f, 0.0f, 0.0f, 0.0f, 255f,
                0.0f, -1.0f, 0.0f, 0.0f, 255f,
                0.0f, 0.0f, -1.0f, 0.0f, 255f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            )
        )

        val destinationColor = Color.parseColor("#FF2A2A2A")
        val lr = (255.0f - Color.red(destinationColor)) / 255.0f
        val lg = (255.0f - Color.green(destinationColor)) / 255.0f
        val lb = (255.0f - Color.blue(destinationColor)) / 255.0f
        val grayscaleMatrix = ColorMatrix(
            floatArrayOf(
                lr, lg, lb, 0f, 0f,  //
                lr, lg, lb, 0f, 0f,  //
                lr, lg, lb, 0f, 0f, 0f, 0f, 0f, 0f, 255f
            )
        )
        grayscaleMatrix.preConcat(inverseMatrix)
        val dr = Color.red(destinationColor)
        val dg = Color.green(destinationColor)
        val db = Color.blue(destinationColor)
        val drf = dr / 255f
        val dgf = dg / 255f
        val dbf = db / 255f
        val tintMatrix = ColorMatrix(
            floatArrayOf(
                drf, 0f, 0f, 0f, 0f, 0f,
                dgf, 0f, 0f, 0f, 0f, 0f,
                dbf, 0f, 0f, 0f, 0f, 0f, 1f, 0f
            )
        )
        tintMatrix.preConcat(grayscaleMatrix)
        val lDestination = drf * lr + dgf * lg + dbf * lb
        val scale = 1f - lDestination
        val translate = 1 - scale * 0.5f
        val scaleMatrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, dr * translate, 0f,
                scale, 0f, 0f, dg * translate, 0f, 0f,
                scale, 0f, db * translate, 0f, 0f, 0f, 1f, 0f
            )
        )
        scaleMatrix.preConcat(tintMatrix)
        val filter = ColorMatrixColorFilter(scaleMatrix)
        mapView.getOverlayManager().getTilesOverlay().setColorFilter(filter)
    }
}