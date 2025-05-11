package com.example.textrecognition.ui

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.textrecognition.util.TranslationClient
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File

@Composable
fun CameraScreen(onTranslateComplete: (String) -> Unit) {
    CameraContent(onTranslateComplete)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraContent(onTranslateComplete: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    var detectedText by remember { mutableStateOf("No text detected yet...") }
    var selectedLanguage by remember { mutableStateOf("Turkish") }
    var isTranslationAvailable by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val screenHeightPx = context.resources.displayMetrics.heightPixels
    val screenHeightDp = with(LocalDensity.current) { screenHeightPx.toDp() }

    val languageOptions = mapOf(
        "Turkish" to "tr",
        "Spanish" to "es",
        "German" to "de",
        "French" to "fr",
        "Chinese" to "zh",
        "Russian" to "ru",
        "Japanese" to "ja",
        "Italian" to "it",
        "Korean" to "ko",
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("Text Scanner") })
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(Color.BLACK)
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_START
                    }.also { previewView ->
                        startCamera(
                            context, lifecycleOwner, previewView, imageCapture
                        )
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeightDp / 2)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        .padding(16.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(detectedText.split("\n")) { line ->
                            Text(
                                text = line,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            captureAndProcessImage(context, imageCapture) { result ->
                                detectedText = result
                                isTranslationAvailable = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text("Extract Text")
                    }

                    Button(
                        onClick = {
                            if (isTranslationAvailable) {
                                val languageCode = languageOptions[selectedLanguage] ?: "tr"
                                TranslationClient.updateTargetLanguage(languageCode)
                                TranslationClient.getTranslator().downloadModelIfNeeded()
                                    .addOnSuccessListener {
                                        translateText(detectedText) { result ->
                                            onTranslateComplete(result) // Navigate to result screen
                                        }
                                    }
                                    .addOnFailureListener {
                                        onTranslateComplete("Failed to download model.")
                                    }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        enabled = isTranslationAvailable
                    ) {
                        Text("Translate")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Language Dropdown
                Box {
                    Button(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Language: $selectedLanguage")
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        languageOptions.keys.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(language) },
                                onClick = {
                                    selectedLanguage = language
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun translateText(text: String, callback: (String) -> Unit) {
    TranslationClient.getTranslator().translate(text)
        .addOnSuccessListener { translatedText ->
            callback(translatedText)
        }
        .addOnFailureListener { exception ->
            exception.printStackTrace()
            callback("Translation failed.")
        }
}

private fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    imageCapture: ImageCapture
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (exc: Exception) {
            exc.printStackTrace()
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun captureAndProcessImage(
    context: Context,
    imageCapture: ImageCapture,
    onTextDetected: (String) -> Unit
) {
    val photoFile = createTempFile(context)
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                exc.printStackTrace()
                onTextDetected("Failed to capture image.")
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                recognizeTextFromImage(context, savedUri, onTextDetected)
            }
        }
    )
}

private fun createTempFile(context: Context): File {
    return File.createTempFile("captured_image", ".jpg", context.cacheDir)
}

private fun recognizeTextFromImage(
    context: Context,
    imageUri: Uri,
    onTextDetected: (String) -> Unit
) {
    try {
        val image = InputImage.fromFilePath(context, imageUri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                onTextDetected(visionText.text.ifEmpty { "No text detected." })
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                onTextDetected("Error detecting text.")
            }
    } catch (e: Exception) {
        e.printStackTrace()
        onTextDetected("Failed to load image for recognition.")
    }
}
