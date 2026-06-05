package com.jsac.sync.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Utility for compressing images before upload
 * Reduces file size significantly (80-90% reduction possible)
 */
object ImageCompression {

    /**
     * Compress bitmap and save to file
     *
     * @param context Android context
     * @param bitmap Bitmap to compress
     * @param maxWidth Maximum width in pixels
     * @param maxHeight Maximum height in pixels
     * @param quality JPEG quality (1-100, default 85)
     * @return Compressed file path
     */
    fun compressAndSave(
        context: Context,
        bitmap: Bitmap,
        maxWidth: Int = 1280,
        maxHeight: Int = 1280,
        quality: Int = 85
    ): Result<String> = try {
        Log.d("ImageCompression", "🖼️ Compressing image - size: ${bitmap.width}x${bitmap.height}")

        // Resize if needed
        val resized = if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
            Log.d("ImageCompression", "📏 Resizing from ${bitmap.width}x${bitmap.height} to max ${maxWidth}x${maxHeight}")
            resizeBitmap(bitmap, maxWidth, maxHeight)
        } else {
            bitmap
        }

        // Save to cache directory
        val file = File(context.cacheDir, "image_${System.currentTimeMillis()}.jpg")

        FileOutputStream(file).use { out ->
            resized.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.flush()
        }

        val originalSize = bitmap.byteCount
        val compressedSize = file.length()
        val reduction = ((originalSize - compressedSize) * 100) / originalSize

        Log.d("ImageCompression", "✅ Compressed: $originalSize → $compressedSize bytes (${reduction}% reduction)")

        Result.success(file.absolutePath)

    } catch (e: Exception) {
        Log.e("ImageCompression", "❌ Compression error: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Resize bitmap proportionally
     */
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val ratio = Math.min(
            maxWidth.toFloat() / bitmap.width,
            maxHeight.toFloat() / bitmap.height
        )

        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Get file size in MB
     */
    fun getFileSizeMB(filePath: String): Double {
        return File(filePath).length() / (1024.0 * 1024.0)
    }

    /**
     * Load bitmap from file path
     */
    fun loadBitmap(filePath: String, maxWidth: Int = 1024, maxHeight: Int = 1024): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)

            options.apply {
                inJustDecodeBounds = false
                inSampleSize = calculateInSampleSize(outWidth, outHeight, maxWidth, maxHeight)
            }

            BitmapFactory.decodeFile(filePath, options)
        } catch (e: Exception) {
            Log.e("ImageCompression", "Error loading bitmap: ${e.message}")
            null
        }
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Int {
        var inSampleSize = 1
        if (height > maxHeight || width > maxWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= maxHeight && halfWidth / inSampleSize >= maxWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}