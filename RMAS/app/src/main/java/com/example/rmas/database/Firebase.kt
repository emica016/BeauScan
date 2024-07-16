package com.example.rmas.database

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

object Firebase {

    fun login(
        usernameOrEmail: String,
        password: String,
        successCallback: () -> Unit,
        failureCallback: () -> Unit,
    ) {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        if(usernameOrEmail.matches(emailRegex.toRegex())) {
            loginWithEmail(usernameOrEmail, password, successCallback, failureCallback)
        }
        else {
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
                Log.e("Login", exception.toString() )
                failureCallback()
            }
    }

    private fun loginWithUsername(
        username: String,
        password: String,
        successCallback: () -> Unit,
        failureCallback: () -> Unit,
    ) {

        Firebase.firestore.collection("usernames").document(username).get().addOnSuccessListener { document ->
            val email = document.get("email") as String?
            if(email != null) {
                Log.e("FIREBASE", "email found: $email")
                loginWithEmail(email, password, successCallback, failureCallback)
            }
            else {
                Log.e("FIREBASE", "email not found: $email")
                failureCallback()
            }

        }.addOnFailureListener {
            Log.e("FIREBASE", "loginWithUsername: $it", )
            failureCallback()
        }
    }

}