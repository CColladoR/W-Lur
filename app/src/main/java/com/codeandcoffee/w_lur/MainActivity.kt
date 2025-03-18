package com.codeandcoffee.w_lur

import OnboardingScreen
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
        setContent {
            WLurTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "onboarding") {
                    composable("onboarding") {
                        OnboardingScreen(onImageSelected = { uri ->
                            val encodedUri = Uri.encode(uri.toString())
                            navController.navigate("image/$encodedUri")
                        })
                    }
                    composable(
                        "image/{imageUri}",
                        arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val imageUriString = backStackEntry.arguments?.getString("imageUri")
                        val imageUri = imageUriString?.let { Uri.parse(it) }
                        val imageViewModel: ImageViewModel = viewModel() // Obtener el ViewModel
                        ImageScreen(
                            imageUri = imageUri,
                            navController = navController,
                            viewModel = imageViewModel
                        )
                    }
                }
            }
        }
    }
}