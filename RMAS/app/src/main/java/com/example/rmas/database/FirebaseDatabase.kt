package com.example.rmas.database

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.example.rmas.data.Place
import com.example.rmas.data.Request
import com.example.rmas.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import kotlin.math.cos

object FirebaseDatabase {

    fun createAccount(
        email: String,
        password: String,
        username: String,
        fullName: String,
        phoneNumber: String,
        image: ImageBitmap,
        points: Int,
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
                                            points,
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
        points: Int,
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
            uploadImageToFirestore(email, username, fullName, phoneNumber, image, points, successCallback, failureCallback)
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
        points: Int,
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
                        points,
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
        points: Int,
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
        user.points = points
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

    fun getUser(uid: String, callback: (User?) -> Unit) {
        Firebase.firestore.collection("users").document(uid).get().addOnSuccessListener { document ->
            if (document.exists()) {
                callback(document.toObject(User::class.java))
            }
        }
    }

    fun updateUser(id: String, fullName: String, phoneNumber: String, image: String, onComplete: (Boolean) -> Unit) {
        val userRef = Firebase.firestore.collection("users").document(id)

        val updates = hashMapOf<String, Any>(
            "fullName" to fullName,
            "phoneNumber" to phoneNumber,
            "image" to image
        )

        userRef.update(updates)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
                onComplete(false)
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

    fun getPlaceInfo(eventID: String, callback: (Place?) -> Unit) {
        Firebase.firestore.collection("places").document(eventID).get().addOnSuccessListener { document ->
            if (document.exists()) {
                callback(document.toObject(Place::class.java))
            }
        }
    }

    fun getPlacesAtCurrentLocation(lat: Double, lng: Double, callback: (List<Place>) -> Unit) {
        val radiusInKm = 0.01 // 10 meters
        val earthRadiusKm = 6371.0

        val latDelta = Math.toDegrees(radiusInKm / earthRadiusKm)
        val lngDelta = Math.toDegrees(radiusInKm / (earthRadiusKm * cos(Math.toRadians(lat))))

        val minLat = lat - latDelta
        val maxLat = lat + latDelta
        val minLng = lng - lngDelta
        val maxLng = lng + lngDelta

        val places = mutableListOf<Place>()

        Firebase.firestore.collection("places")
            .whereGreaterThanOrEqualTo("latitude", minLat)
            .whereLessThanOrEqualTo("latitude", maxLat)
            .whereGreaterThanOrEqualTo("longitude", minLng)
            .whereLessThanOrEqualTo("longitude", maxLng)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val place = document.toObject(Place::class.java)
                    place.type = document.getString("type")!!
                    place.description = document.getString("description")!!
                    place.purpose = document.getString("purpose")!!
                    place.comments = (document.get("comments") as HashMap<String, String>?)!!
                    place.ratingNum = document.getDouble("rating")!!.toInt()
                    place.rating = document.getDouble("rating")!!
                    places.add(place)
                }
                callback(places)
            }
            .addOnFailureListener {
                Log.e("Firebase", "getEventsAtCurrentLocation: $it", )
            }
    }

    fun getAllPlaces(callback: (List<Place>) -> Unit) {
        val places = mutableListOf<Place>()
        Firebase.firestore.collection("places").get().addOnSuccessListener { documents ->
            for (document in documents) {
                val place = document.toObject(Place::class.java)
                place.type = document.getString("type")!!
                place.description = document.getString("description")!!
                place.purpose = document.getString("purpose")!!
                place.comments = (document.get("comments") as HashMap<String, String>?)!!
                place.ratingNum = document.getDouble("rating")!!.toInt()
                place.rating = document.getDouble("rating")!!
                places.add(place)
            }
            callback(places)
        }
    }

    fun getCurrentUser() : String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    fun setUserAttendance(eventID: String) {
        Firebase.firestore.collection("attendance").document(eventID).collection("users").document(
            getCurrentUser()!!).set(mapOf("attended" to true))
    }

    fun checkUserAttendance(eventID: String, callback: (Boolean) -> Unit) {
        Firebase.firestore.collection("attendance").document(eventID).collection("users").document(
            getCurrentUser()!!).get().addOnSuccessListener { document ->
            if(document.exists()) {
                callback(true)
            } else {
                callback(false)
            }
        }
    }

    fun incrementUserScore(userID: String) {
        Firebase.firestore.collection("leaderboard").document(userID).update("score", FieldValue.increment(1))
            .addOnFailureListener {
                Log.e("FIREBASE", "incrementUserScore: $it", )
                Firebase.firestore.collection("leaderboard").document(userID).set(mapOf("score" to 1)).addOnFailureListener { exception ->
                    Log.e("FIREBASE", "setUserScore: $exception", )
                }
            }
    }

    fun sendResponse(place: Place, requester: String) {
        // Implement the logic to send the response to the requester
        // For example, you can use Firebase Firestore to store the response
        val responseRef = Firebase.firestore.collection("responses").document(place.name)
        responseRef.set(mapOf("requester" to requester))

    }

    fun saveRequest(request: Request) {
        Firebase.firestore.collection("requests").document(request.id).set(request)
    }

    fun getRequest(uid: String, callback: (Request?) -> Unit) {
        Firebase.firestore.collection("requests").document(uid).get().addOnSuccessListener { document ->
            if (document.exists()) {
                callback(document.toObject(Request::class.java))
            } else {
                callback(null)
            }
        }
    }

    fun getAllRequests(): List<Request> {
        val requests = mutableListOf<Request>() // Initialize the list
        Firebase.firestore.collection("requests").get().addOnSuccessListener { documents ->
            for (document in documents) {
                val request = document.toObject(Request::class.java)
                requests.add(request) // Add each document to the list
            }}
            return requests // Return the list
        }


}





