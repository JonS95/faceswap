package com.jons95.faceswap.ml

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.max
import kotlin.math.min

/**
 * Geometry-based face swap: aligns the source face onto the target face using an
 * affine transform anchored on eyes + mouth, then composites it through a feathered
 * mask so the blend has soft edges, with a simple mean-color match to the target's
 * skin tone. No neural network involved - this is the v1 "fast path" described in
 * the project guide; a learned model can replace this step later for higher fidelity.
 */
object GeometricSwap {

    fun swap(sourceBitmap: Bitmap, sourceFace: Face, targetBitmap: Bitmap, targetFace: Face): Bitmap? {
        val srcPoints = anchorPoints(sourceFace) ?: return null
        val dstPoints = anchorPoints(targetFace) ?: return null

        val matrix = Matrix()
        val solved = matrix.setPolyToPoly(
            floatArrayOf(srcPoints[0].x, srcPoints[0].y, srcPoints[1].x, srcPoints[1].y, srcPoints[2].x, srcPoints[2].y),
            0,
            floatArrayOf(dstPoints[0].x, dstPoints[0].y, dstPoints[1].x, dstPoints[1].y, dstPoints[2].x, dstPoints[2].y),
            0,
            3
        )
        if (!solved) return null

        val result = targetBitmap.copy(Bitmap.Config.ARGB_8888, true) ?: return null
        val warpedSource = Bitmap.createBitmap(result.width, result.height, Bitmap.Config.ARGB_8888)
        Canvas(warpedSource).drawBitmap(sourceBitmap, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))

        val mask = buildFeatheredMask(result.width, result.height, targetFace)

        val matched = colorMatch(warpedSource, result, mask, targetFace.boundingBox)
        if (matched !== warpedSource) warpedSource.recycle()

        val maskedLayer = applyMask(matched, mask)
        matched.recycle()
        mask.recycle()

        val canvas = Canvas(result)
        canvas.drawBitmap(maskedLayer, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        maskedLayer.recycle()
        return result
    }

    /** Returns [leftEye, rightEye, mouthCenter] in bitmap pixel coordinates, or null if unavailable. */
    private fun anchorPoints(face: Face): List<PointF>? {
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
        val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
        val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position
        if (leftEye == null || rightEye == null || mouthLeft == null || mouthRight == null) return null
        val mouthCenter = PointF((mouthLeft.x + mouthRight.x) / 2f, (mouthLeft.y + mouthRight.y) / 2f)
        return listOf(leftEye, rightEye, mouthCenter)
    }

    private fun buildFeatheredMask(width: Int, height: Int, face: Face): Bitmap {
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mask)
        val contour = face.getContour(FaceContour.FACE)?.points
        val path = Path()
        if (contour != null && contour.size > 2) {
            path.moveTo(contour[0].x, contour[0].y)
            for (i in 1 until contour.size) path.lineTo(contour[i].x, contour[i].y)
            path.close()
        } else {
            val box = face.boundingBox
            val expand = box.width() * 0.08f
            path.addOval(
                RectF(box.left - expand, box.top - expand, box.right + expand, box.bottom + expand),
                Path.Direction.CW
            )
        }
        val blurRadius = max(1f, face.boundingBox.width() * 0.12f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(path, paint)
        return mask
    }

    private fun applyMask(source: Bitmap, mask: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(source, 0f, 0f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(mask, 0f, 0f, paint)
        return out
    }

    /** Shifts the warped source's mean RGB (within the face region) to match the target's, a cheap stand-in for proper color transfer. */
    private fun colorMatch(warpedSource: Bitmap, target: Bitmap, mask: Bitmap, region: Rect): Bitmap {
        val left = max(0, region.left)
        val top = max(0, region.top)
        val right = min(target.width, region.right)
        val bottom = min(target.height, region.bottom)
        if (right <= left || bottom <= top) return warpedSource

        val (srcMean, srcCount) = meanColorIn(warpedSource, mask, left, top, right, bottom)
        val (dstMean, dstCount) = meanColorIn(target, mask, left, top, right, bottom)
        if (srcCount == 0 || dstCount == 0) return warpedSource

        val deltaR = dstMean[0] - srcMean[0]
        val deltaG = dstMean[1] - srcMean[1]
        val deltaB = dstMean[2] - srcMean[2]

        val adjusted = warpedSource.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(adjusted.width * adjusted.height)
        adjusted.getPixels(pixels, 0, adjusted.width, 0, 0, adjusted.width, adjusted.height)
        for (y in top until bottom) {
            for (x in left until right) {
                val idx = y * adjusted.width + x
                val p = pixels[idx]
                val a = Color.alpha(p)
                if (a == 0) continue
                val r = clamp(Color.red(p) + deltaR)
                val g = clamp(Color.green(p) + deltaG)
                val b = clamp(Color.blue(p) + deltaB)
                pixels[idx] = Color.argb(a, r, g, b)
            }
        }
        adjusted.setPixels(pixels, 0, adjusted.width, 0, 0, adjusted.width, adjusted.height)
        return adjusted
    }

    private fun meanColorIn(bitmap: Bitmap, mask: Bitmap, left: Int, top: Int, right: Int, bottom: Int): Pair<DoubleArray, Int> {
        var sumR = 0.0
        var sumG = 0.0
        var sumB = 0.0
        var count = 0
        val stepX = max(1, (right - left) / 64)
        val stepY = max(1, (bottom - top) / 64)
        var y = top
        while (y < bottom) {
            var x = left
            while (x < right) {
                if (x in 0 until mask.width && y in 0 until mask.height) {
                    val maskAlpha = Color.alpha(mask.getPixel(x, y))
                    if (maskAlpha > 32) {
                        val p = bitmap.getPixel(x, y)
                        sumR += Color.red(p)
                        sumG += Color.green(p)
                        sumB += Color.blue(p)
                        count++
                    }
                }
                x += stepX
            }
            y += stepY
        }
        return if (count == 0) {
            Pair(doubleArrayOf(0.0, 0.0, 0.0), 0)
        } else {
            Pair(doubleArrayOf(sumR / count, sumG / count, sumB / count), count)
        }
    }

    private fun clamp(value: Double): Int = max(0.0, min(255.0, value)).toInt()
}
