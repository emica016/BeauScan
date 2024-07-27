package com.example.rmas.database

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.example.rmas.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

object FirebaseDatabase {

    fun createAccount(
        email: String,
        password: String,
        username: String,
        fullName: String,
        phoneNumber: String,
        image: ImageBitmap,
        successCallback: () -> Unit,
        failureCallback: () -> Unit
    ) {
        val auth: FirebaseAuth = FirebaseAuth.getInstance()
        // Create account
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Log.d("Register", "SUCCESSFUL")
                // Check if username or email already exists
                Firebase.firestore.collection("usernames").document(username).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            Log.e("FirebaseDatabase", "Username already exists")
                            failureCallback()
                        } else {
                            Firebase.firestore.collection("emails").document(email).get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        Log.e("FirebaseDatabase", "Email already exists")
                                        failureCallback()
                                    } else {
                                        bindUsernameToEmail(
                                            email,
                                            username,
                                            fullName,
                                            phoneNumber,
                                            image,
                                            successCallback,
                                            failureCallback
                                        )
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("FirebaseDatabase", "Failed to check if email exists")
                                    failureCallback()
                                }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirebaseDatabase", "Failed to check if username exists")
                        failureCallback()
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("Auth", exception.toString())
                failureCallback()
            }
    }


    private fun bindUsernameToEmail(
        email: String,
        username: String,
        fullName: String,
        phoneNumber: String,
        image: ImageBitmap,
        successCallback: () -> Unit,
        failureCallback: () -> Unit
    ) {
        val auth: FirebaseAuth = FirebaseAuth.getInstance()
        // Bind username to email
        Firebase.firestore.collection("usernames").document(username).set(
            hashMapOf(
                "email" to email
            )
        ).addOnSuccessListener {
            Log.d("UsernameToPassword", "SUCCESSFUL")
            uploadImageToFirestore(email, username, fullName, phoneNumber, image, successCallback, failureCallback)
        }.addOnFailureListener {// Revert registration process if anything fails
            // Delete account if bind fails
            auth.currentUser!!.delete()
            failureCallback()
        }
    }

    fun uploadImageToFirestore(
        email: String,
        username: String,
        fullName: String,
        phoneNumber: String,
        image: ImageBitmap,
        successCallback: () -> Unit,
        failureCallback: () -> Unit
    ) {
        val ref =
            FirebaseStorage.getInstance().reference.child("images/${FirebaseAuth.getInstance().currentUser!!.uid}.jpg")

        val stream = ByteArrayOutputStream()
        image.asAndroidBitmap().compress(Bitmap.CompressFormat.JPEG, 100, stream)

        ref.putBytes(stream.toByteArray())
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()
                    // Handle the success (e.g., save the URL or update UI)
                    Log.d("FirebaseStorage", "Upload successful, URL: $downloadUrl")
                    writeAdditionalDataOnRegistration(
                        email,
                        username,
                        fullName,
                        phoneNumber,
                        downloadUrl,
                        successCallback,
                        failureCallback
                    )
                }
            }
            .addOnFailureListener { exception ->
                // Handle the failure
                Log.e("FirebaseStorage", "Upload failed", exception)
                Firebase.firestore.collection("usernames").document(username).delete()
                // Delete account
                val auth: FirebaseAuth = FirebaseAuth.getInstance()
                auth.currentUser!!.delete()
                failureCallback()
            }
    }

    private fun writeAdditionalDataOnRegistration(
        email: String,
        username: String,
        fullName: String,
        phoneNumber: String,
        profilePictureURL: String,
        successCallback: () -> Unit,
        failureCallback: () -> Unit
    ) {
        val auth: FirebaseAuth = FirebaseAuth.getInstance()
        // Write additional user data to the database
        val user = User()
        user.id = auth.currentUser!!.uid
        user.email = email
        user.username = username
        user.fullName = fullName
        user.phoneNumber = phoneNumber
        user.image = profilePictureURL
        Firebase.firestore.collection("users").document(auth.currentUser!!.uid).set(user)
            .addOnSuccessListener {
                Log.d("writeAdditionalData", "SUCCESSFUL")
                successCallback()
            }.addOnFailureListener { // Revert registration process if anything fails
            // Remove profile picture from firestore
            FirebaseStorage.getInstance().reference.child("images/${FirebaseAuth.getInstance().currentUser!!.uid}.jpg")
                .delete()
            // Unbind username from email
            Firebase.firestore.collection("usernames").document(username).delete()
            // Delete account
            auth.currentUser!!.delete()
            failureCallback()
        }
    }

    fun login(
        usernameOrEmail: String,
        password: String,
        successCallback: () -> Unit,
        failureCallback: () -> Unit,
    ) {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        if (usernameOrEmail.matches(emailRegex.toRegex())) {
            loginWithEmail(usernameOrEmail, password, successCallback, failureCallback)
        } else {
            loginWithUsername(usernameOrEmail, password, successCallback, failureCallback)
        }
    }

    private fun loginWithEmail(
        email: String,
        password: String,
        successCallback: () -> Unit,
        failureCallback: () -> Unit,
    ) {
        val auth: FirebaseAuth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Log.i("Login", "Login successful")
                successCallback()
            }
            .addOnFailureListener { exception ->
                Log.e("Login", exception.toString())
                failureCallback()
            }
    }

    private fun loginWithUsername(
        username: String,
        password: String,
        successCallback: () -> Unit,
        failureCallback: () -> Unit,
    ) {

        Firebase.firestore.collection("usernames").document(username).get()
            .addOnSuccessListener { document ->
                val email = document.get("email") as String?
                if (email != null) {
                    Log.e("FIREBASE", "email found: $email")
                    loginWithEmail(email, password, successCallback, failureCallback)
                } else {
                    Log.e("FIREBASE", "email not found: $email")
                    failureCallback()
                }

            }.addOnFailureListener {
            Log.e("FIREBASE", "loginWithUsername: $it",)
            failureCallback()
        }
    }
}