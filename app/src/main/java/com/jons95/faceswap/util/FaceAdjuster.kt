package com.jons95.faceswap.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

data class FaceAdjustments(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val warmth: Float = 0f,
    val blendOpacity: Float = 1f
) {
    val isDefault: Boolean get() =
        brightness == 0f && contrast == 1f && saturation == 1f && warmth == 0f && blendOpacity == 1f
}

object FaceAdjuster {

    fun apply(swapped: Bitmap, original: Bitmap, adjustments: FaceAdjustments): Bitmap {
        if (adjustments.isDefault) return swapped

        val w = swapped.width
        val h = swapped.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        canvas.drawBitmap(original, 0f, 0f, null)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val cm = ColorMatrix()

        if (adjustments.saturation != 1f) {
            val sat = ColorMatrix()
            sat.setSaturation(adjustments.saturation)
            cm.postConcat(sat)
        }

        if (adjustments.contrast != 1f) {
            val c = adjustments.contrast
            val t = 128f * (1f - c)
            val contrastMatrix = ColorMatrix(floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(contrastMatrix)
        }

        if (adjustments.brightness != 0f) {
            val b = adjustments.brightness * 255f
            val brightnessMatrix = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, b,
                0f, 1f, 0f, 0f, b,
                0f, 0f, 1f, 0f, b,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(brightnessMatrix)
        }

        if (adjustments.warmth != 0f) {
            val w = adjustments.warmth * 30f
            val warmthMatrix = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, w,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, -w,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(warmthMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(cm)
        paint.alpha = (adjustments.blendOpacity * 255).toInt().coerceIn(0, 255)

        canvas.drawBitmap(swapped, 0f, 0f, paint)
        return out
    }
}
