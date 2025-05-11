package com.example.textrecognition.ui

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    var translatedText by remember { mutableStateOf("") }

    NavHost(navController = navController, startDestination = "camera") {
        composable("camera") {
            CameraScreen { result ->
                translatedText = result
                navController.navigate("result")
            }
        }
        composable("result") {
            TranslationResultScreen(navController, translatedText)
        }
    }
}
