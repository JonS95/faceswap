package com.jons95.faceswap.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

private const val MAX_DIMENSION = 1280

object ImageUtils {

    /** Loads a bitmap from a content Uri, applies EXIF rotation, and downsamples large images. */
    fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver

        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri) ?: return null
        boundsStream.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
        val decodeOptions = android.graphics.BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val rawBitmap = resolver.openInputStream(uri)?.use { stream ->
            android.graphics.BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null

        val rotationDegrees = resolver.openInputStream(uri)?.use { stream ->
            readExifRotation(stream)
        } ?: 0

        return if (rotationDegrees != 0) {
            rotateBitmap(rawBitmap, rotationDegrees)
        } else {
            rawBitmap
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        val largestSide = maxOf(width, height)
        while (largestSide / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun readExifRotation(stream: java.io.InputStream): Int {
        return try {
            val exif = ExifInterface(stream)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (_: Exception) {
            0
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    /** Saves a bitmap to the device gallery via MediaStore. Returns the resulting Uri, or null on failure. */
    fun saveToGallery(context: Context, bitmap: Bitmap): Uri? {
        val filename = "faceswap_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FaceSwap")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return null
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        } ?: return null
        return uri
    }

    /** Writes the bitmap to app cache and returns a content:// Uri suitable for sharing. */
    fun shareableUriFor(context: Context, bitmap: Bitmap): Uri {
        val cacheDir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(cacheDir, "faceswap_share_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun shareIntentFor(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
