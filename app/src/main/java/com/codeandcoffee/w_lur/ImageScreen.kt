package com.codeandcoffee.w_lur

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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
    // Mapear sliderValue (0f a 1f) a un rango de 0dp a 10dp
    val blurRadiusDp = sliderValue * 50f // Valor en DP
    val blurRadiusPx = with(density) { blurRadiusDp.dp.toPx() } // Convertir a PX solo para el ViewModel

    val context = LocalContext.current
    val contentResolver = context.contentResolver

    Log.d("ImageScreen", "URI recibida en ImageScreen: $imageUri")

    var displayedBitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }
    var loadAttempted by remember { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        if (imageUri != null && !loadAttempted) {
            loadAttempted = true
            Log.d("ImageScreen", "LaunchedEffect - Intentando cargar Bitmap directamente desde URI")
            withContext(Dispatchers.IO) {
                try {
                    val inputStream = contentResolver.openInputStream(imageUri)
                    displayedBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    Log.d("ImageScreen", "LaunchedEffect - Bitmap cargado con éxito desde URI")
                } catch (e: Exception) {
                    Log.e("ImageScreen", "LaunchedEffect - Error al cargar Bitmap desde URI", e)
                    displayedBitmap = null
                }
            }
        } else if (imageUri == null) {
            displayedBitmap = null
            loadAttempted = false
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
        Log.d("ImageScreen", "Box - displayedBitmap: $displayedBitmap")
        if (imageUri != null) {
            displayedBitmap?.let { bitmap ->
                Log.d("ImageScreen", "displayedBitmap?.let - bitmap no es nulo")
                val modifier = Modifier.fillMaxSize()
                val blurRadius = blurRadiusDp.dp // Usamos DP directamente para la previsualización

                val imageModifier = if (sliderValue > 0f) {
                    modifier.blur(radius = blurRadius)
                } else {
                    modifier
                }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Selected Image",
                    modifier = imageModifier,
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
                        // Pasamos blurRadiusPx en píxeles al ViewModel
                        viewModel.saveBlurredImage(imageUri, blurRadiusPx) {
                            navController.popBackStack()
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