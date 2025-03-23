package com.codeandcoffee.w_lur

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.codeandcoffee.w_lur.ui.theme.WLurTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Habilitar modo edge-to-edge antes de setContent
        enableEdgeToEdge()

        // Forzar la transparencia de las barras antes de renderizar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // Asegurarnos de que el fondo de la ventana sea transparente
        window.setBackgroundDrawable(null)

        setContent {
            val isDarkTheme = isSystemInDarkTheme()

            // Ajustar el color de los íconos según el tema
            WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightStatusBars = !isDarkTheme // Negro en claro, blanco en oscuro
                isAppearanceLightNavigationBars = !isDarkTheme // Negro en claro, blanco en oscuro
            }

            WLurTheme(useDarkTheme = isDarkTheme) {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "onboarding") {
                    composable("onboarding") {
                        OnboardingScreen(
                            onImageSelected = { uri ->
                                val encodedUri = Uri.encode(uri.toString())
                                navController.navigate("image/$encodedUri")
                            },
                            navController = navController
                        )
                    }
                    composable(
                        "image/{imageUri}",
                        arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val imageUriString = backStackEntry.arguments?.getString("imageUri")
                        val imageUri = imageUriString?.let { Uri.parse(it) }
                        val imageViewModel: ImageViewModel = viewModel()
                        ImageScreen(
                            imageUri = imageUri,
                            navController = navController,
                            viewModel = imageViewModel,
                            onImageSaved = {
                                navController.previousBackStackEntry?.savedStateHandle?.set("imageSaved", true)
                            }
                        )
                    }
                }
            }
        }
    }
}