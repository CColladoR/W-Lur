package com.codeandcoffee.w_lur

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class ImageViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext: Context
        get() = getApplication<Application>().applicationContext

    fun saveImage(bitmap: Bitmap, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            saveBitmapToGallery(bitmap)
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    private suspend fun saveBitmapToGallery(bitmap: Bitmap) {
        val filename = "W_Lur_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/W_Lur")
        }

        var uri: Uri? = null
        try {
            uri = appContext.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                appContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                // Notify the system that a new image has been added to the MediaStore
                MediaScannerConnection.scanFile(appContext, arrayOf(uri.toString()), arrayOf("image/png"), null)
                Log.d("ImageViewModel", "Imagen guardada en la galería: $uri")
            } else {
                Log.e("ImageViewModel", "Error al insertar la URI de la imagen")
            }
        } catch (e: IOException) {
            uri?.let { appContext.contentResolver.delete(it, null, null) }
            Log.e("ImageViewModel", "Error al guardar la imagen", e)
        }
    }

    // New function to save the blurred image in the background
    fun saveBlurredImage(imageUri: Uri?, blurRadiusPx: Float, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            if (imageUri == null) {
                withContext(Dispatchers.Main) { onComplete() }
                return@launch
            }
            try {
                val inputStream = appContext.contentResolver.openInputStream(imageUri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null && blurRadiusPx > 0) {
                    val adjustedBlurRadius = blurRadiusPx / 10f // Try dividing by 10
                    val blurredBitmap = applyBlur(originalBitmap, adjustedBlurRadius)
                    saveBitmapToGallery(blurredBitmap)
                } else if (originalBitmap != null) {
                    saveBitmapToGallery(originalBitmap)
                }
            } catch (e: Exception) {
                Log.e("ImageViewModel", "Error processing image for saving", e)
            } finally {
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }

    private fun applyBlur(sourceBitmap: Bitmap, blurRadiusPx: Float): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.maskFilter = BlurMaskFilter(blurRadiusPx, BlurMaskFilter.Blur.NORMAL)

        val resultBitmap = Bitmap.createBitmap(sourceBitmap.width, sourceBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        canvas.drawBitmap(sourceBitmap, 0f, 0f, paint)

        return resultBitmap
    }
}