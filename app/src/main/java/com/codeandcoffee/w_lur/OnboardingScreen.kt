package com.codeandcoffee.w_lur

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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.codeandcoffee.w_lur.ui.theme.endGradient
import com.codeandcoffee.w_lur.ui.theme.startGradient
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(
    onImageSelected: (Uri) -> Unit,
    navController: NavController
) {
    val context = LocalContext.current

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionState = rememberPermissionState(permission)

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onImageSelected(uri)
        } else {
            Log.d("OnboardingScreen", "El usuario canceló la selección de imagen")
        }
    }

    var showPermissionRationaleDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var imageSaved by remember { mutableStateOf(false) }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getStateFlow("imageSaved", false)?.collect { saved ->
            if (saved) {
                imageSaved = true
                savedStateHandle.set("imageSaved", false)
            }
        }
    }

    LaunchedEffect(imageSaved) {
        if (imageSaved) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "Imagen guardada en la galería",
                    duration = SnackbarDuration.Short
                )
                imageSaved = false
            }
        }
    }

    val isDarkTheme = isSystemInDarkTheme()
    val baseColor = if (isDarkTheme) {
        Color(0xFF1A237E)
    } else {
        Color(0xff91bceb)
    }

    val animatedColors = remember(isDarkTheme) { generateColorScheme(baseColor, 6) }
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
                animatedCenterX.value * context.resources.displayMetrics.widthPixels.toFloat(),
                animatedCenterY.value * context.resources.displayMetrics.heightPixels.toFloat()
            )
        },
        radius = with(density) { animatedRadius.value * 1000.dp.toPx() }
    )

    // Definir la fuente local Rubik Glitch
    val rubikGlitchFont = FontFamily(
        Font(R.font.rgreg, weight = FontWeight.Normal)
    )

    val interFont = FontFamily(
        Font(R.font.inter, weight = FontWeight.Normal)
    )

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = endGradient,
                    contentColor = Color.White
                ) {
                    Text(
                        data.visuals.message,
                        fontFamily = interFont)
                }
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
                .padding(paddingValues),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 68.sp,
                    fontFamily = rubikGlitchFont,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = stringResource(R.string.onboarding_help_text),
                    fontSize = 18.sp,
                    fontFamily = interFont,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                MinimalButton(
                    onClick = {
                        if (permissionState.status.isGranted) {
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } else if (permissionState.status.shouldShowRationale) {
                            showPermissionRationaleDialog = true
                        } else {
                            permissionState.launchPermissionRequest()
                        }
                    },
                    text = stringResource(R.string.select_image_button)
                )

                if (showPermissionRationaleDialog) {
                    AlertDialog(
                        onDismissRequest = { showPermissionRationaleDialog = false },
                        title = { Text(stringResource(R.string.permission_dialog_title)) },
                        text = { Text(stringResource(R.string.permission_dialog_message)) },
                        confirmButton = {
                            MinimalButton(
                                onClick = {
                                    showPermissionRationaleDialog = false
                                    permissionState.launchPermissionRequest()
                                },
                                text = stringResource(R.string.permission_dialog_ok)
                            )
                        },
                        dismissButton = {
                            MinimalButton(
                                onClick = { showPermissionRationaleDialog = false },
                                text = stringResource(R.string.permission_dialog_cancel)
                            )
                        }
                    )
                }
            }
        }
    }
}

fun generateColorScheme(baseColor: Color, count: Int): List<Color> {
    val hsl = baseColor.toHsl()
    val colors = mutableListOf<Color>()
    val isDark = hsl[2] < 0.5f

    for (i in 0 until count) {
        val newHue = (hsl[0] + (i * (360f / count))) % 360f
        val newSaturation = (hsl[1] + (i * (0.2f / count))) % 1f
        val newLightness = if (isDark) {
            (hsl[2] - (i * (0.1f / count))).coerceIn(0f, 0.4f)
        } else {
            (hsl[2] + (i * (0.4f / count))).coerceIn(0.5f, 1f)
        }

        colors.add(Color.hsl(newHue, newSaturation.coerceIn(0f, 1f), newLightness))
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
        return floatArrayOf(0f, 0f, l)
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

@Composable
fun MinimalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String
) {
    val interFont2 = FontFamily(
        Font(R.font.inter, weight = FontWeight.Normal)
    )

    val isDarkTheme = isSystemInDarkTheme()
    val buttonColor = if (isDarkTheme) {
        startGradient
    } else {
        endGradient
    }
    Button(
        onClick = onClick,
        modifier = modifier
            .height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Medium,
            fontFamily = interFont2,
            fontSize = 14.sp
        )
    }
}