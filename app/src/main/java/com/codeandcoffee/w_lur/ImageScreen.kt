package com.codeandcoffee.w_lur

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.codeandcoffee.w_lur.ui.theme.endGradient
import com.codeandcoffee.w_lur.ui.theme.startGradient
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ImageScreen(
    imageUri: Uri?,
    navController: NavController,
    viewModel: ImageViewModel = viewModel(),
    onImageSaved: () -> Unit
) {
    var sliderValue by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val blurRadiusDp = sliderValue * 25f
    val blurRadiusPx = with(density) { blurRadiusDp.dp.toPx() }

    val context = LocalContext.current
    val contentResolver = context.contentResolver

    var displayedBitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }
    var loadAttempted by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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
        displayedBitmap?.let { bitmap ->
            val modifier = Modifier.fillMaxSize()
            val blurRadius = blurRadiusDp.dp
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
        } ?: run {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF121212)),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    Text("Cargando imagen...", color = Color.White)
                } else {
                    Text("Error: No image selected", color = Color.Red)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isSaving) {
                SavingIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .padding(horizontal = 16.dp)
                )
            } else {
                SquigglySlider(
                    value = sliderValue,
                    onValueChange = { newValue -> sliderValue = newValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .padding(horizontal = 16.dp),
                    trackColor = Color.White,
                    activeTrackColor = startGradient,
                    thumbColor = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            MinimalButton(
                onClick = {
                    if (permissionState.status.isGranted) {
                        isSaving = true
                        viewModel.saveBlurredImage(imageUri, blurRadiusPx) {
                            isSaving = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Image saved to your gallery",
                                    duration = SnackbarDuration.Short
                                )
                            }
                            onImageSaved()
                            navController.popBackStack()
                        }
                    } else if (permissionState.status.shouldShowRationale) {
                        showPermissionRationaleDialog = true
                    } else {
                        permissionState.launchPermissionRequest()
                    }
                },
                enabled = !isSaving,
                text = "Save Image"
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) { data ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                containerColor = endGradient, // Color de fondo personalizado
                contentColor = Color.White // Color del texto para contraste
            ) {
                Text(data.visuals.message)
            }
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

@Composable
fun SavingIndicator(modifier: Modifier = Modifier) {
    val animation = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animation.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = { it }),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    LinearProgressIndicator(
        progress = { animation.value },
        modifier = modifier.clipToBounds(),
        color = endGradient,
        trackColor = Color.White.copy(alpha = 0.3f),
    )
}