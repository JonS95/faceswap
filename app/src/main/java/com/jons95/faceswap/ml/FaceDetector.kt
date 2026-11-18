package com.jons95.faceswap.ml

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Thin wrapper around ML Kit's on-device face detector. */
class FaceDetector {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()
    )

    /** Runs detection on a bitmap and blocks (on an IO/Default dispatcher) until results are ready. */
    suspend fun detectFaces(bitmap: Bitmap): List<Face> = withContext(Dispatchers.Default) {
        val image = InputImage.fromBitmap(bitmap, 0)
        Tasks.await(detector.process(image))
    }

    /** Convenience: returns the largest detected face (by bounding box area), or null if none found. */
    suspend fun detectLargestFace(bitmap: Bitmap): Face? {
        val faces = detectFaces(bitmap)
        return faces.maxByOrNull { it.boundingBox.width().toLong() * it.boundingBox.height().toLong() }
    }

    fun close() {
        detector.close()
    }
}
