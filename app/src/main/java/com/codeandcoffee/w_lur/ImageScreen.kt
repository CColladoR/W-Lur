package com.codeandcoffee.w_lur

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.graphics.applyCanvas
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ImageScreen(
    imageUri: Uri?,
    navController: NavController,
    viewModel: ImageViewModel = viewModel()
) {
    var sliderValue by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val blurRadius = with(density) { sliderValue * 32.dp.toPx() }

    val context = LocalContext.current

    Log.d("ImageScreen", "URI recibida en ImageScreen: $imageUri") // Añade este log

    val request = ImageRequest.Builder(context)
        .data(imageUri)
        .size(Size.ORIGINAL)
        .listener(
            onStart = {
                Log.d("ImageScreen", "Iniciando carga de imagen con Coil")
            },
            onSuccess = { request, result ->
                Log.d("ImageScreen", "Imagen cargada con éxito: ${result.drawable}")
            },
            onError = { request, throwable ->
                Log.e("ImageScreen", "Error cargando imagen con Coil", throwable)
            }
        )
        .build()


    // Cargar la imagen original (usando Coil)
    val painter = rememberAsyncImagePainter(
        model = request
    )

    // ... (resto del código antes del LaunchedEffect) ...
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var blurredBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // LaunchedEffect para cargar el bitmap original y aplicar desenfoque
    LaunchedEffect(sliderValue, painter.state) {
        when (painter.state) {
            is AsyncImagePainter.State.Loading -> {
                // Opcional: Mostrar un indicador de carga mientras se carga la imagen original.
                // No es estrictamente necesario para este ejemplo simplificado.
                Log.d("ImageScreen", "Cargando imagen...")
                originalBitmap = null // Importante resetear
                blurredBitmap = null
            }
            is coil.request.SuccessResult -> {
                Log.d("ImageScreen", "Imagen cargada correctamente")
                val loadedBitmap = (painter.state as coil.request.SuccessResult).drawable.toBitmap()
                originalBitmap = loadedBitmap

                withContext(Dispatchers.Default) {
                    val currentBitmap = originalBitmap // Usar variable local
                    if (currentBitmap != null) {
                        val newBlurredBitmap =
                            currentBitmap.copy(currentBitmap.config ?: Bitmap.Config.ARGB_8888, true)
                                .apply {
                                    val radius = blurRadius.toInt()
                                    if (radius > 0) {
                                        blurBitmap(this, radius)
                                    }
                                }
                        blurredBitmap = newBlurredBitmap
                    }
                }
            }
            is coil.request.ErrorResult -> {
                // ¡MUY IMPORTANTE! Mostrar un error si Coil falla
                Log.e("ImageScreen", "Error al cargar la imagen con Coil: ${(painter.state as coil.request.ErrorResult).throwable}")
                originalBitmap = null //Asegurarse
                blurredBitmap = null
            }
            else -> {
                Log.d("ImageScreen", "Estado del painter: ${painter.state}") // Añadido para depuración
                originalBitmap = null //Asegurarse
                blurredBitmap = null
            }
        }
    }

    // --- Permisos para guardar la imagen ---
    val savePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    }
    val permissionState = rememberPermissionState(savePermission)
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }

   Box(modifier = Modifier.fillMaxSize()) {
        if (imageUri != null) {
            // Mostrar la imagen (usa el currentBitmap)
            displayedBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Selected Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } ?: Text("Cargando imagen...", modifier = Modifier.align(Alignment.Center)) // Mensaje de carga

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
                onValueChange = { newValue -> sliderValue = newValue },
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
                        // Usa currentBitmap, que ya tiene el desenfoque aplicado
                        displayedBitmap?.let { btm ->
                            viewModel.saveImage(btm) {
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

        // Diálogo de explicación de permisos (si es necesario)
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


// Función de utilidad para el desenfoque (PRIVADA)
private fun blurBitmap(bitmap: Bitmap, radius: Int) {
    val scaledWidth = bitmap.width / 8
    val scaledHeight = bitmap.height / 8
    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, false)

    val canvas = Canvas(scaledBitmap)
    canvas.drawColor(Color.Transparent.copy(alpha = 0.1f).toArgb())

    val paint = Paint()
    paint.isAntiAlias = true

    val colorMatrix = android.graphics.ColorMatrix().apply {
        val alpha = 0.5f + (radius / 32f) * 0.5f
        setScale(1f, 1f, 1f, alpha.coerceIn(0f, 1f))
    }
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)

    for (i in 0 until radius) {
        canvas.drawBitmap(scaledBitmap, 0f, 0f, paint)
    }
    val finalBitmap = Bitmap.createScaledBitmap(scaledBitmap, bitmap.width, bitmap.height, false)
    bitmap.applyCanvas { //Se aplica el canvas sobre el bitmap original.
        drawBitmap(finalBitmap, 0f, 0f, null)
    }
}