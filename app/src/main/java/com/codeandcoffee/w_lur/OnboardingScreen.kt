// OnboardingScreen.kt
import android.Manifest
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeandcoffee.w_lur.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(onImageSelected: (Uri) -> Unit) { // Callback

    val context = LocalContext.current

    // --- Gestión de Permisos (con Accompanist) ---
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES // Android 13 (API 33) y superior
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE // Android 12 (API 31-32) y anteriores
    }

    val permissionState = rememberPermissionState(permission)

    // --- Selector de Imágenes (ActivityResultContracts) ---
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) } // Guarda la URI de la imagen

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia() // Usa PickVisualMedia
    ) { uri: Uri? ->
        // --- Cambio aquí: Llamar al callback ---
        if (uri != null) {
            onImageSelected(uri) // Llama al callback
        } else {
            // Opcional: Manejar el caso en que el usuario cancela
            Log.d("OnboardingScreen", "El usuario canceló la selección de imagen")
        }
        // ----------------------------------------
    }

    // --- Estado para el diálogo de explicación de permisos ---
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }


    // ---  Animación del Gradiente (sin cambios) ---
    val baseColor = Color(0xff91bceb)
    val animatedColors = remember { generateColorScheme(baseColor, 6) }
    val infiniteTransition = rememberInfiniteTransition(label = "")

    val animatedCenterX = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    val animatedCenterY = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    val animatedRadius = infiniteTransition.animateFloat(
        initialValue = 3f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    val density = LocalDensity.current
    val brush = Brush.radialGradient(
        colors = animatedColors,
        center = with(density) {
            Offset(
                animatedCenterX.value * LocalContext.current.resources.displayMetrics.widthPixels.toFloat(),
                animatedCenterY.value * LocalContext.current.resources.displayMetrics.heightPixels.toFloat()
            )
        },
        radius = with(density) { animatedRadius.value * 1000.dp.toPx() } // Un radio grande para cubrir la pantalla

    )

    // --- UI (con el botón modificado) ---
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121212)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(brush),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = stringResource(R.string.onboarding_help_text),
                fontSize = 18.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- Botón (con la lógica de permisos) ---
            Button(
                onClick = {
                    if (permissionState.status.isGranted) {
                        // 1. Permiso YA concedido: lanzar el selector
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) // Usa PickVisualMediaRequest
                    } else if (permissionState.status.shouldShowRationale) {
                        // 2. Mostrar diálogo de explicación
                        showPermissionRationaleDialog = true
                    } else {
                        // 3. Solicitar permiso directamente
                        permissionState.launchPermissionRequest()
                    }
                }
            ) {
                Text(text = stringResource(R.string.select_image_button))
            }

            // --- Diálogo de explicación de permisos (si es necesario) ---
            if (showPermissionRationaleDialog) {
                AlertDialog(
                    onDismissRequest = { showPermissionRationaleDialog = false },
                    title = { Text(stringResource(R.string.permission_dialog_title)) },
                    text = { Text(stringResource(R.string.permission_dialog_message)) },
                    confirmButton = {
                        Button(onClick = {
                            showPermissionRationaleDialog = false
                            permissionState.launchPermissionRequest() // Solicita el permiso
                        }) {
                            Text(stringResource(R.string.permission_dialog_ok))
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showPermissionRationaleDialog = false }) {
                            Text(stringResource(R.string.permission_dialog_cancel))
                        }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    //   OnboardingScreen() //No se le puede pasar un callback a la preview
}

fun generateColorScheme(baseColor: Color, count: Int): List<Color> {
    val hsl = baseColor.toHsl()
    val colors = mutableListOf<Color>()

    for (i in 0 until count) {
        // Variamos el tono ligeramente y la luminosidad
        val newHue = (hsl[0] + (i * (360f / count))) % 360f
        val newLightness = (hsl[2] + (i * (0.4f / count))) % 1f
        val newSaturation = (hsl[1] + (i* (0.2f / count))) % 1f

        colors.add(Color.hsl(newHue, newSaturation.coerceIn(0f,1f), newLightness.coerceIn(0f, 1f)))
    }
    return colors
}

fun Color.toHsl(): FloatArray {
    val r = this.red
    val g = this.green
    val b = this.blue

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    val l = (max + min) / 2

    if (delta == 0f) {
        return floatArrayOf(0f, 0f, l) // Es gris, tono y saturación irrelevantes
    }

    val s = if (l < 0.5f) {
        delta / (max + min)
    } else {
        delta / (2 - max - min)
    }

    val h = when {
        r == max -> (g - b) / delta
        g == max -> 2 + (b - r) / delta
        else -> 4 + (r - g) / delta
    }

    var hue = (h * 60).toFloat()
    if (hue < 0) hue += 360f

    return floatArrayOf(hue, s, l)
}