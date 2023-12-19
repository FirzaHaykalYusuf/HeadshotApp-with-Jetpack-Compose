package com.firza.headshotapp.ui.screens

import android.content.Context
import android.graphics.ColorFilter
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import coil.compose.rememberImagePainter
import com.firza.headshotapp.R
import com.firza.headshotapp.ui.viewmodels.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files.createFile
import java.util.*

@Composable
fun CameraScreen(
    navController: NavHostController,
    lifecycleOwner: LifecycleOwner,
    sharedViewModel: SharedViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var lastTakenPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // State untuk menangani tampilan dialog pratinjau
    var showPreviewDialog by remember { mutableStateOf(false) }

    fun handleClosePreviewDialog() {
        showPreviewDialog = false
    }

    // State untuk loading indicator
    var isLoading by remember { mutableStateOf(false) }

    fun handleLoadingDialog() {
        isLoading = false
    }

    // State untuk kamera saat ini dan ImageCapture
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var imageCapture: ImageCapture by remember {
        mutableStateOf(
            ImageCapture.Builder().build()
        )
    }

    // State untuk menyimpan status izin kamera
    var hasCameraPermission by remember { mutableStateOf(false) }

    // Launcher untuk meminta izin kamera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    // Meminta izin kamera
    LaunchedEffect(key1 = true) {
        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    val previewView = remember { PreviewView(context) } // Ingat instance PreviewView

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_FRONT_CAMERA) }

            // Ini harus di dalam LaunchedEffect agar diperbarui ketika `lensFacing` berubah
            LaunchedEffect(lensFacing) {
                cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
                try {
                    imageCapture = ImageCapture.Builder().build()
                    val previewUseCase = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        previewUseCase,
                        imageCapture
                    )
                } catch (e: Exception) {
                    Log.e("CameraScreen", "Switching camera failed", e)
                }
            }


            Image(
                painter = painterResource(id = R.drawable.ic_change_camera), // Ganti dengan drawable ikon ganti kamera Anda
                contentDescription = "Switch Camera",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(50.dp)
                    .clickable {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                            CameraSelector.LENS_FACING_BACK
                        } else {
                            CameraSelector.LENS_FACING_FRONT
                        }
                        // Re-inisialisasi imageCapture dengan konfigurasi baru
                        imageCapture = ImageCapture
                            .Builder()
                            .build()
                        // Re-bind use cases dengan kamera yang dipilih
                        cameraProviderFuture
                            .get()
                            .apply {
                                unbindAll()
                                try {
                                    val newCameraSelector =
                                        CameraSelector
                                            .Builder()
                                            .requireLensFacing(lensFacing)
                                            .build()
                                    val previewUseCase = Preview
                                        .Builder()
                                        .build()
                                        .also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }
                                    bindToLifecycle(
                                        lifecycleOwner,
                                        newCameraSelector,
                                        previewUseCase,
                                        imageCapture
                                    )
                                } catch (e: Exception) {
                                    Log.e("CameraScreen", "Switching camera failed", e)
                                }
                            }
                    }
            )
        } else {
            Text("Tidak ada akses kamera. Silakan berikan izin kamera.")
        }

        // Tombol untuk menangkap gambar
        Button(
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    val photoFile = createFile(context, "jpg")
                    val outputFileOptions =
                        ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    imageCapture.takePicture(
                        outputFileOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                                Log.d("CameraScreen", "Image Saved at $savedUri")
                                sharedViewModel.removeBackgroundFromImage(context, savedUri)
                                handleLoadingDialog()
                                showPreviewDialog = true
                            }

                            override fun onError(exc: ImageCaptureException) {
                                Log.e("CameraScreen", "Image capture failed", exc)
                                handleLoadingDialog()
                            }
                        }
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 30.dp)
        ) {
            Text("Capture")
        }

        // Dialog loading
        if (isLoading) {
            Dialog(onDismissRequest = {}) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(100.dp).padding(16.dp)
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // Dialog untuk pratinjau gambar yang telah diproses
        val processedImageUri by sharedViewModel.processedImageUri.observeAsState()
        if (processedImageUri != null && showPreviewDialog) {
            handleLoadingDialog()
            Dialog(onDismissRequest = { handleClosePreviewDialog() }) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = rememberImagePainter(processedImageUri),
                        contentDescription = "Preview Photo",
                        modifier = Modifier.fillMaxSize()
                    )

                    // Tombol hapus di pojok kiri atas
                    IconButton(
                        onClick = { handleClosePreviewDialog() }, // Menutup dialog
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 10.dp, bottom = 350.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(50.dp)
                        )
                    }

                    Button(
                        onClick = {
                            // Handle confirm action
                            sharedViewModel.setImageUri(processedImageUri)
                            navController.navigate("formScreen")
                            handleClosePreviewDialog() // Tutup dialog setelah konfirmasi
                        },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp)
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

//// Dialog untuk pratinjau gambar yang telah diproses
//        val processedImageUri by sharedViewModel.processedImageUri.observeAsState()
//        processedImageUri?.let { uri ->
//            Dialog(onDismissRequest = { /* Handle dismiss */ }) {
//                Box(modifier = Modifier.fillMaxSize()) {
//                    Image(
//                        painter = rememberImagePainter(uri),
//                        contentDescription = "Preview Photo",
//                        modifier = Modifier.fillMaxSize()
//                    )
//                    Button(
//                        onClick = {
//                            // Handle confirm action
//                            sharedViewModel.setImageUri(uri)
//                            navController.navigate("formScreen")
//                            showPreviewDialog = false
//                        },
//                        modifier = Modifier.align(Alignment.BottomCenter)
//                    ) {
//                        Text("Confirm")
//                    }
//                }
//            }
//        }
//    }
//}

//        // Tombol untuk menangkap gambar
//        Button(
//            onClick = {
//                coroutineScope.launch {
//                    val photoFile = createFile(context, "jpg")
//                    val outputFileOptions =
//                        ImageCapture.OutputFileOptions.Builder(photoFile).build()
//                    imageCapture.takePicture(
//                        outputFileOptions,
//                        ContextCompat.getMainExecutor(context),
//                        object : ImageCapture.OnImageSavedCallback {
//                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
//                                val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
//                                Log.d("CameraScreen", "Image Saved at $savedUri")
//
//                                // Proses gambar untuk menghapus latar belakang
//                                savedUri?.let { uri ->
//                                    sharedViewModel.removeBackgroundFromImage(context, uri)
//                                }
//                            }
//
//                            override fun onError(exc: ImageCaptureException) {
//                                Log.e("CameraScreen", "Image capture failed", exc)
//                            }
//                        }
//                    )
//                }
//            },
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .padding(16.dp)
//        ) {
//            Text("Capture")
//        }
//
//        // Dialog untuk pratinjau gambar yang telah diproses
//        val processedImageUri by sharedViewModel.processedImageUri.observeAsState()
//        processedImageUri?.let { uri ->
//            Dialog(onDismissRequest = { lastTakenPhotoUri = null }) {
//                Box(modifier = Modifier.fillMaxSize()) {
//                    Image(
//                        painter = rememberImagePainter(uri),
//                        contentDescription = "Preview Photo",
//                        modifier = Modifier.fillMaxSize()
//                    )
//                    Button(
//                        onClick = {
//                            // Handle confirm action
//                            navController.navigate("formScreen")
//                            sharedViewModel.setImageUri(uri) // Reset URI setelah navigasi
//                        },
//                        modifier = Modifier.align(Alignment.BottomCenter)
//                    ) {
//                        Text("Confirm")
//                    }
//                }
//            }
//        }
//    }
//}

//        // Dialog untuk pratinjau gambar
//        lastTakenPhotoUri?.let { uri ->
//            Dialog(onDismissRequest = { lastTakenPhotoUri = null }) {
//                Box(modifier = Modifier.fillMaxSize()) {
//                    Image(
//                        painter = rememberImagePainter(uri),
//                        contentDescription = "Preview Photo",
//                        modifier = Modifier.fillMaxSize()
//                    )
//                    Button(
//                        onClick = {
//                            sharedViewModel.setImageUri(uri)
//                            navController.navigate("formScreen")
//                            lastTakenPhotoUri = null
//                        },
//                        modifier = Modifier.align(Alignment.BottomCenter)
//                    ) {
//                        Text("Confirm")
//                    }
//                }
//            }
//        }
//    }
//}

//fun saveImageUriToPreferences(context: Context, imageUri: Uri) {
//    val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
//    sharedPreferences.edit().putString("imageUri", imageUri.toString()).apply()
//}

fun createFile(context: Context, extension: String): File {
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile("JPEG_${timeStamp}_", ".$extension", storageDir)
}