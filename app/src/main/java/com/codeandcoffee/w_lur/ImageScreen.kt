package com.codeandcoffee.w_lur

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import kotlin.math.roundToInt

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ImageScreen(
    imageUri: Uri?,
    navController: NavController,
    viewModel: ImageViewModel = viewModel()
) {
    var sliderValue by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val blurRadiusDp = with(density) { sliderValue * 15.dp.toPx() } // Ajusta este factor
    val context = LocalContext.current

    Log.d("ImageScreen", "URI recibida en ImageScreen: $imageUri")

    val request = ImageRequest.Builder(context)
        .data(imageUri)
        .size(Size.ORIGINAL)
        .listener(
            onStart = { Log.d("ImageScreen", "Iniciando carga de imagen con Coil") },
            onSuccess = { _, _ -> Log.d("ImageScreen", "Imagen cargada con éxito") },
            onError = { _, result -> Log.e("ImageScreen", "Error cargando imagen con Coil", result.throwable) }
        )
        .build()

    val painter = rememberAsyncImagePainter(model = request)

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var blurredBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var displayedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(painter.state) {
        if (painter.state is AsyncImagePainter.State.Success) {
            originalBitmap = (painter.state as AsyncImagePainter.State.Success).result.drawable.toBitmap()
            displayedBitmap = originalBitmap?.asImageBitmap()
            Log.d("ImageScreen", "Imagen original cargada: $originalBitmap")
        } else if (painter.state is AsyncImagePainter.State.Error) {
            Log.e("ImageScreen", "Error al cargar la imagen con Coil")
            originalBitmap = null
            blurredBitmap = null
            displayedBitmap = null
        } else if (painter.state is AsyncImagePainter.State.Loading) {
            Log.d("ImageScreen", "Cargando imagen...")
            originalBitmap = null
            blurredBitmap = null
            displayedBitmap = null
        }
    }

    LaunchedEffect(sliderValue, originalBitmap) {
        if (originalBitmap != null) {
            val radius = blurRadiusDp.roundToInt()
            Log.d("ImageScreen", "Blur LaunchedEffect - sliderValue: $sliderValue, radius: $radius")
            if (radius > 0) {
                withContext(Dispatchers.Default) {
                    try {
                        val bitmapToBlur = originalBitmap!!.copy(Bitmap.Config.ARGB_8888, true)
                        val blurred = horizontalBlur(bitmapToBlur, radius)
                        val verticallyBlurred = verticalBlur(blurred, radius)
                        blurredBitmap = verticallyBlurred
                        displayedBitmap = verticallyBlurred?.asImageBitmap()
                        Log.d("ImageScreen", "Desenfoque aplicado con Two-Pass Box Blur")
                    } catch (e: Exception) {
                        Log.e("ImageScreen", "Error applying Two-Pass Box Blur", e)
                        displayedBitmap = originalBitmap?.asImageBitmap()
                    }
                }
            } else {
                displayedBitmap = originalBitmap?.asImageBitmap()
                blurredBitmap = null
                Log.d("ImageScreen", "Radio de desenfoque es 0, mostrando imagen original")
            }
        }
    }

    val savePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    }
    val permissionState = rememberPermissionState(savePermission)
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (imageUri != null) {
            displayedBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = "Selected Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } ?: Text("Cargando imagen...", modifier = Modifier.align(Alignment.Center))
        } else {
            Text("Error: No image selected", color = Color.Red)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SquigglySlider(
                value = sliderValue,
                onValueChange = { newValue ->
                    sliderValue = newValue
                    Log.d("ImageScreen", "Valor del slider cambiado a: $newValue")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                trackColor = Color.White,
                activeTrackColor = Color.Cyan,
                thumbColor = Color.White,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (permissionState.status.isGranted) {
                        displayedBitmap?.let { btm ->
                            viewModel.saveImage(btm.asAndroidBitmap()) {
                                navController.popBackStack()
                            }
                        }
                    } else if (permissionState.status.shouldShowRationale) {
                        showPermissionRationaleDialog = true
                    } else {
                        permissionState.launchPermissionRequest()
                    }
                },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text("Guardar imagen")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showPermissionRationaleDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionRationaleDialog = false },
                title = { Text("Permiso Requerido") },
                text = { Text("La aplicación necesita permiso para guardar imágenes en la galería.") },
                confirmButton = {
                    Button(onClick = {
                        showPermissionRationaleDialog = false
                        permissionState.launchPermissionRequest()
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    Button(onClick = { showPermissionRationaleDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

private fun horizontalBlur(bitmap: Bitmap, radius: Int): Bitmap {
    if (radius <= 0) return bitmap

    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val resultPixels = pixels.copyOf()

    for (y in 0 until height) {
        for (x in 0 until width) {
            var rSum = 0
            var gSum = 0
            var bSum = 0
            var count = 0

            for (i in -radius..radius) {
                val xPos = x + i
                if (xPos in 0 until width) {
                    val index = y * width + xPos
                    val pixel = pixels[index]
                    rSum += (pixel shr 16) and 0xFF
                    gSum += (pixel shr 8) and 0xFF
                    bSum += pixel and 0xFF
                    count++
                }
            }

            val avgR = rSum / count
            val avgG = gSum / count
            val avgB = bSum / count

            resultPixels[y * width + x] = (avgR shl 16) or (avgG shl 8) or avgB or (0xFF shl 24)
        }
    }

    val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    resultBitmap.setPixels(resultPixels, 0, width, 0, 0, width, height)
    return resultBitmap
}

private fun verticalBlur(bitmap: Bitmap, radius: Int): Bitmap {
    if (radius <= 0) return bitmap

    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val resultPixels = pixels.copyOf()

    for (x in 0 until width) {
        for (y in 0 until height) {
            var rSum = 0
            var gSum = 0
            var bSum = 0
            var count = 0

            for (j in -radius..radius) {
                val yPos = y + j
                if (yPos in 0 until height) {
                    val index = yPos * width + x
                    val pixel = pixels[index]
                    rSum += (pixel shr 16) and 0xFF
                    gSum += (pixel shr 8) and 0xFF
                    bSum += pixel and 0xFF
                    count++
                }
            }

            val avgR = rSum / count
            val avgG = gSum / count
            val avgB = bSum / count

            resultPixels[y * width + x] = (avgR shl 16) or (avgG shl 8) or avgB or (0xFF shl 24)
        }
    }

    val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    resultBitmap.setPixels(resultPixels, 0, width, 0, 0, width, height)
    return resultBitmap
}