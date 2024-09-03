package com.example.rmas.services

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Objects
import java.util.UUID


class CameraService(private val context: Context) {
    private var imageUri: Uri = Uri.EMPTY


    @SuppressLint("SimpleDateFormat")
    private fun createImageFile(): Uri {
        val timeStamp = SimpleDateFormat("yyyy_MM_dd_HH:mm:ss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val image = File.createTempFile(
            imageFileName,
            ".jpg",
            context.externalCacheDir
        )

        return FileProvider.getUriForFile(
            Objects.requireNonNull(context),
            context.packageName + ".provider", image
        )
    }

    private var cameraLauncher: ManagedActivityResultLauncher<Uri, Boolean>? = null
    private var imagePickerLauncher:  ManagedActivityResultLauncher<Intent, ActivityResult>? = null

    @Composable
    fun Setup(callback: (uri: Uri) -> Int) {
        cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
            callback(imageUri)
        }

        imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.e("URI", result.data?.data.toString())
            if (result.resultCode == Activity.RESULT_OK) {

                Log.e("URI", result.data.toString())
                if(result.data?.data != null) {
                    imageUri = result.data?.data!!
                    callback(imageUri)
                }
            }
        }
    }

    fun takePicture() {
        imageUri = createImageFile()
        cameraLauncher?.launch(imageUri)
    }

    fun uploadPicture() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        imagePickerLauncher?.launch(intent)
    }

    fun loadImageBitmapFromUri(uri: Uri): ImageBitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source)
        return bitmap.asImageBitmap()
    }
}

suspend fun uploadImageToStorage(imageUri: Uri, context: Context): String? {
    // Referenca na Firebase Storage
    val storage = FirebaseStorage.getInstance()
    val storageRef = storage.reference

    // Kreirajte jedinstveno ime za sliku
    val imageRef = storageRef.child("images/${UUID.randomUUID()}.jpg")

    return try {
        // Učitajte sliku na Firebase Storage
        val uploadTask = imageRef.putFile(imageUri).await()

        // Kada je učitavanje uspešno, dobijte URL slike
        val downloadUrl = imageRef.downloadUrl.await()
        downloadUrl.toString()
    } catch (e: Exception) {
        // Obradite greške
        Log.e("UploadImage", "Upload failed", e)
        null
    }
}