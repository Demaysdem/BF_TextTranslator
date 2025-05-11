package com.example.textrecognition.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.textrecognition.util.TranslationClient
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TextRecognitionScreen() {
    val cameraPermissionState: PermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val context = LocalContext.current
    val navController = rememberNavController()

    MainContent(
        hasPermission = cameraPermissionState.status.isGranted,
        onRequestPermission = cameraPermissionState::launchPermissionRequest,
        navController = navController
    )

    LaunchedEffect(Unit) {
        downloadTranslationModel(context)
    }
}

private fun downloadTranslationModel(context: Context) {
    TranslationClient.getTranslator().downloadModelIfNeeded()
        .addOnSuccessListener {
            Toast.makeText(context, "Model downloaded successfully", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { exception ->
            exception.printStackTrace()
            Toast.makeText(context, "Model download failed", Toast.LENGTH_SHORT).show()
        }
}

@Composable
private fun MainContent(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    navController: androidx.navigation.NavHostController
) {
    if (hasPermission) {
        AppNavigation(navController = navController)
    } else {
        NoPermissionScreen(onRequestPermission)
    }
}

@Preview
@Composable
private fun Preview_MainContent() {
    // Preview without navigation in design time
    MainContent(
        hasPermission = true,
        onRequestPermission = {},
        navController = rememberNavController()
    )
}
