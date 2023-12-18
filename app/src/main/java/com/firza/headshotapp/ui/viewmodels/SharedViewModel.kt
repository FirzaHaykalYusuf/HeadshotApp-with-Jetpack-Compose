package com.firza.headshotapp.ui.viewmodels

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.firza.headshotapp.db.AppDatabase
import com.firza.headshotapp.db.entity.UserEntity
import com.firza.headshotapp.repository.UserRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class SharedViewModel(application: Application) : AndroidViewModel(application) {
    private val _imageUri = MutableLiveData<Uri?>()
    val imageUri: LiveData<Uri?> get() = _imageUri

    private val _processedImageUri = MutableLiveData<Uri>()
    val processedImageUri: LiveData<Uri> = _processedImageUri

    // Data tambahan untuk dikirim ke FormScreen
    private val _userData = MutableLiveData<Pair<String, String>>()
    val userData: LiveData<Pair<String, String>> = _userData

    fun setProcessedImageUri(uri: Uri) {
        _processedImageUri.value = uri
    }

    fun setUserData(name: String, phone: String) {
        _userData.value = Pair(name, phone)
    }


    private val _segmentationMask = MutableLiveData<SegmentationMask?>()
    val segmentationMask: LiveData<SegmentationMask?> get() = _segmentationMask

    // Fungsi untuk mengupdate segmentationMask
    fun updateSegmentationMask(mask: SegmentationMask?) {
        _segmentationMask.value = mask
    }

    fun setImageUri(uri: Uri?) {
        _imageUri.value = uri
    }

    fun removeBackgroundFromImage(context: Context, imageUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                processImageWithMLKit(context, imageUri)
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Error removing background", e)
            }
        }
    }

    private val userRepository: UserRepository

    init {
        val userDao = AppDatabase.getDatabase(application, viewModelScope).userDao()
        userRepository = UserRepository(userDao)
        getUsers() // Mengambil data pengguna saat ViewModel diinisialisasi
    }

    private val _userList = MutableLiveData<List<UserEntity>>()
    val userList: LiveData<List<UserEntity>> = _userList

    fun insertUser(name: String, phone: String, imageUri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.insert(UserEntity(name = name, phone = phone, imageUri = imageUri))
            getUsers() // Mengambil ulang data pengguna setelah menambahkan pengguna baru
        }
    }

    fun getUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            val users = userRepository.getAllUsers()
            _userList.postValue(users)
        }
    }

    fun deleteUser(userId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.deleteUser(userId)
            getUsers() // Refresh the user list after deletion
        }
    }

    fun updateUser(name: String, phone: String, imageUri: String, userId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val userEntity = UserEntity(id = userId, name = name, phone = phone, imageUri = imageUri)
            userRepository.updateUser(userEntity)
            getUsers() // Refresh the list
        }
    }

    fun processImageWithMLKit(context: Context, imageUri: Uri) {
        val inputImage = InputImage.fromFilePath(context, imageUri)
        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .build()
        val segmenter = Segmentation.getClient(options)

        segmenter.process(inputImage)
            .addOnSuccessListener { segmentationMask ->
                val originalBitmap =
                    MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
                val resultBitmap = createBitmapFromMask(segmentationMask, originalBitmap)

                // Menyimpan hasilnya ke penyimpanan dan memperbarui LiveData
                val resultUri = saveImageToStorage(context, resultBitmap)
                _processedImageUri.postValue(resultUri)

                // Simpan URI yang diproses ke SharedPreferences
                saveImageUriToPreferences(context, resultUri)
            }
            .addOnFailureListener { e ->
                Log.e("SharedViewModel", "Error in image segmentation", e)
            }
    }

    private fun createBitmapFromMask(mask: SegmentationMask, originalBitmap: Bitmap): Bitmap {
        val resultBitmap = Bitmap.createBitmap(
            originalBitmap.width,
            originalBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(resultBitmap)

        // Skalakan maskBitmap untuk cocok dengan ukuran originalBitmap jika diperlukan
        val scaledMaskBitmap = Bitmap.createScaledBitmap(
            mask.toBitmap(),
            originalBitmap.width,
            originalBitmap.height,
            true
        )

        // Menggambar bitmap asli
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)

        // Menggambar maskBitmap di atasnya
        val paint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawBitmap(scaledMaskBitmap, 0f, 0f, paint)

        return resultBitmap
    }

    private fun SegmentationMask.toBitmap(): Bitmap {
        val maskBitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
        val buffer = this.buffer
        buffer.rewind() // Reset buffer position

        val pixels = IntArray(this.width * this.height)
        for (y in 0 until this.height) {
            for (x in 0 until this.width) {
                val pixelValue = buffer.float
                pixels[y * this.width + x] = if (pixelValue > 0.5f) {
                    0xFF000000.toInt() // Piksel bagian dari subjek
                } else {
                    0x00000000 // Piksel transparan
                }
            }
        }
        maskBitmap.setPixels(pixels, 0, this.width, 0, 0, this.width, this.height)
        return maskBitmap
    }

    fun saveImageUriToPreferences(context: Context, imageUri: Uri) {
        val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("imageUri", imageUri.toString()).apply()
    }

    private fun saveImageToStorage(context: Context, bitmap: Bitmap): Uri {
        val filename = "processed_image_${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null
        var imageUri: Uri? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver?.also { resolver ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                imageUri =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            imageUri = Uri.fromFile(image)
            fos = FileOutputStream(image)
        }
        fos?.let { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        fos?.flush()
        fos?.close()
        return imageUri ?: throw IOException("Gagal menyimpan gambar")
    }
}