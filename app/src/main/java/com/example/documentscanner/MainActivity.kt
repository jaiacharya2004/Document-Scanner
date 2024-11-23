package com.example.documentscanner

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.documentscanner.ui.theme.DocumentScannerTheme
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure the scanner options
        val options = GmsDocumentScannerOptions.Builder()
            // Scanner mode line omitted as it's not mandatory
            .setPageLimit(5)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .build()

        val scanner = GmsDocumentScanning.getClient( options)

        setContent {
            DocumentScannerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
                    var pdfUri by remember { mutableStateOf<Uri?>(null) }

                    // Define scanner launcher
                    val scannerLauncher = rememberLauncherForActivityResult(
                        contract = StartIntentSenderForResult()
                    ) { result ->
                        if (result.resultCode == RESULT_OK) {
                            val scanningResult =
                                GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                            imageUris = scanningResult?.pages?.mapNotNull { it.imageUri } ?: emptyList()

                            // Save the first page as a PDF (if available)
                            scanningResult?.pages?.mapNotNull { it.imageUri }?.let { uris ->
                                // If you need to save a PDF, you'll need to generate it manually.
                                if (uris.isNotEmpty()) {
                                    val pdfFile = File(filesDir, "scan.pdf")
                                    FileOutputStream(pdfFile).use { fos ->
                                        uris.forEach { imageUri ->
                                            contentResolver.openInputStream(imageUri)?.use { inputStream ->
                                                inputStream.copyTo(fos) // Example: You may want to process images into a PDF format.
                                            }
                                        }
                                    }
                                    pdfUri = Uri.fromFile(pdfFile)
                                }
                            }

                        } else {
                            Toast.makeText(
                                applicationContext,
                                "Scanning was canceled or failed",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Display scanned images
                        imageUris.forEach { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        pdfUri?.let {
                            Text(text = "PDF saved at: $it")
                        }

                        // Scan button
                        Button(onClick = {
                            scanner.getStartScanIntent(this@MainActivity)
                                .addOnSuccessListener { intentSender ->
                                    scannerLauncher.launch(
                                        IntentSenderRequest.Builder(intentSender).build()
                                    )
                                }
                                .addOnFailureListener { exception ->
                                    Toast.makeText(
                                        applicationContext,
                                        exception.message ?: "Error starting scanner",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }) {
                            Text(text = "Scan Document")
                        }
                    }
                }
            }
        }
    }
}
