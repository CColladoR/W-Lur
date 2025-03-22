package com.codeandcoffee.w_lur

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
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
import kotlin.math.exp

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
            uri = appContext.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )
            if (uri != null) {
                appContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                MediaScannerConnection.scanFile(
                    appContext,
                    arrayOf(uri.toString()),
                    arrayOf("image/png"),
                    null
                )
                Log.d("ImageViewModel", "Imagen guardada en la galería: $uri")
            } else {
                Log.e("ImageViewModel", "Error al insertar la URI de la imagen")
            }
        } catch (e: IOException) {
            uri?.let { appContext.contentResolver.delete(it, null, null) }
            Log.e("ImageViewModel", "Error al guardar la imagen", e)
        }
    }

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

                if (originalBitmap != null) {
                    Log.d("ImageViewModel", "Aplicando desenfoque de $blurRadiusPx px a la imagen")

                    val resultBitmap = if (blurRadiusPx > 0) {
                        try {
                            applyBlur(originalBitmap, blurRadiusPx)
                        } catch (e: Exception) {
                            Log.e("ImageViewModel", "Error al aplicar desenfoque", e)
                            originalBitmap
                        }
                    } else {
                        originalBitmap
                    }

                    saveBitmapToGallery(resultBitmap)
                    if (resultBitmap != originalBitmap) {
                        resultBitmap.recycle() // Liberar memoria
                    }
                }
                originalBitmap?.recycle() // Liberar memoria
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
        val radius = blurRadiusPx.toInt().coerceAtLeast(1) // Aseguramos un mínimo de 1
        Log.d("ImageViewModel", "Aplicando desenfoque con radio: $radius px")

        val width = sourceBitmap.width
        val height = sourceBitmap.height
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val kernelSize = radius * 2 + 1
        val pixels = IntArray(width * height)
        sourceBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val blurredPixels = IntArray(width * height)

        // Generar un kernel Gaussian para un desenfoque más intenso
        val sigma = radius / 3f // Sigma ajustado para un efecto más fuerte
        val kernel = FloatArray(kernelSize) { i ->
            val x = i - radius
            exp(-(x * x) / (2 * sigma * sigma)) // Fórmula Gaussian
        }
        val kernelSum = kernel.sum()

        // Desenfoque horizontal
        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0f
                var g = 0f
                var b = 0f
                var a = 0f

                for (i in -radius..radius) {
                    val newX = (x + i).coerceIn(0, width - 1)
                    val pixel = pixels[y * width + newX]
                    val weight = kernel[i + radius] / kernelSum
                    r += Color.red(pixel) * weight
                    g += Color.green(pixel) * weight
                    b += Color.blue(pixel) * weight
                    a += Color.alpha(pixel) * weight
                }

                blurredPixels[y * width + x] = Color.argb(
                    a.toInt().coerceIn(0, 255),
                    r.toInt().coerceIn(0, 255),
                    g.toInt().coerceIn(0, 255),
                    b.toInt().coerceIn(0, 255)
                )
            }
        }

        // Desenfoque vertical
        val tempPixels = blurredPixels.copyOf()
        for (x in 0 until width) {
            for (y in 0 until height) {
                var r = 0f
                var g = 0f
                var b = 0f
                var a = 0f

                for (i in -radius..radius) {
                    val newY = (y + i).coerceIn(0, height - 1)
                    val pixel = tempPixels[newY * width + x]
                    val weight = kernel[i + radius] / kernelSum
                    r += Color.red(pixel) * weight
                    g += Color.green(pixel) * weight
                    b += Color.blue(pixel) * weight
                    a += Color.alpha(pixel) * weight
                }

                blurredPixels[y * width + x] = Color.argb(
                    a.toInt().coerceIn(0, 255),
                    r.toInt().coerceIn(0, 255),
                    g.toInt().coerceIn(0, 255),
                    b.toInt().coerceIn(0, 255)
                )
            }
        }

        outputBitmap.setPixels(blurredPixels, 0, width, 0, 0, width, height)
        return outputBitmap
    }
}