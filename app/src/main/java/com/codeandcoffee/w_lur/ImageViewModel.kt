package com.codeandcoffee.w_lur

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

class ImageViewModel(application: Application) : AndroidViewModel(application) {

    fun saveImage(bitmap: Bitmap, onImageSaved: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val resolver = getApplication<Application>().contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "blurred_image_${System.currentTimeMillis()}.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/W-Lur")
                }
            }

            var imageUri: Uri? = null
            var outputStream: OutputStream? = null

            try {
                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw Exception("Failed to create MediaStore entry")

                outputStream = resolver.openOutputStream(imageUri)
                    ?: throw Exception("Failed to open output stream")

                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)) {
                    throw Exception("Failed to save bitmap")
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Imagen guardada", Toast.LENGTH_SHORT).show()
                    onImageSaved()
                }
            } catch (e: Exception) {
                imageUri?.let { resolver.delete(it, null, null) }
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                outputStream?.close()
            }
        }
    }
}