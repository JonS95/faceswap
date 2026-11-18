package com.jons95.faceswap.pipeline

import android.graphics.Bitmap
import com.jons95.faceswap.ml.FaceDetector
import com.jons95.faceswap.ml.GeometricSwap

sealed class SwapResult {
    data class Success(val bitmap: Bitmap) : SwapResult()
    data object NoFaceInSource : SwapResult()
    data object NoFaceInTarget : SwapResult()
    data class Failed(val message: String) : SwapResult()
}

/** Ties face detection and the swap engine together into one call the UI layer can await. */
class SwapPipeline(private val faceDetector: FaceDetector) {

    suspend fun run(sourceBitmap: Bitmap, targetBitmap: Bitmap): SwapResult {
        val sourceFace = faceDetector.detectLargestFace(sourceBitmap)
            ?: return SwapResult.NoFaceInSource
        val targetFace = faceDetector.detectLargestFace(targetBitmap)
            ?: return SwapResult.NoFaceInTarget

        val output = GeometricSwap.swap(sourceBitmap, sourceFace, targetBitmap, targetFace)
            ?: return SwapResult.Failed("Could not align faces (missing eye/mouth landmarks).")

        return SwapResult.Success(output)
    }
}
