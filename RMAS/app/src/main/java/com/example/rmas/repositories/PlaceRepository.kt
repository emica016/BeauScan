package com.example.rmas.data

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class PlaceRepository(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val placeCollection = db.collection("places")
    private val userCollection = db.collection("users")

    fun savePlace(place: Place) {
        val placeCollection = db.collection("places")
        place.id?.let {
            placeCollection.document(it).set(place)
        }
    }


    fun getAllPlaces(): Query {
        return placeCollection.orderBy("dateCreated", Query.Direction.DESCENDING)
    }

    fun getPlace(id: String, callback: (Place?) -> Unit) {
        placeCollection.document(id).get()
            .addOnSuccessListener { document ->
                val place = document.toObject(Place::class.java)
                callback(place)
            }
            .addOnFailureListener { e ->
                Log.d("PlaceRepository", "Error fetching place: ${e.message}")
                callback(null)
            }
    }

    // U PlaceRepository.kt
    fun saveResponse(response: Response) {
        val responsesRef = db.collection("responses")
        responsesRef.add(response)
            .addOnSuccessListener {
                Log.d("PlaceRepository", "Response successfully written!")
            }
            .addOnFailureListener { e ->
                Log.e("PlaceRepository", "Error writing response", e)
            }
    }

    suspend fun getRequestById(requestId: String): Request? {
        return try {
            val documentSnapshot = db.collection("requests").document(requestId).get().await()
            documentSnapshot.toObject(Request::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getPlaceById(placeId: String): Task<DocumentSnapshot> {
        return db.collection("places").document(placeId).get()
    }

    fun getLatestPlaceForUser(userId: String, callback: (Place?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("places")
            .whereEqualTo("creatorID", userId)
            .orderBy("dateCreated", Query.Direction.DESCENDING)
            .orderBy("timeCreated", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    callback(null)
                } else {
                    val document = result.documents.first()
                    val place = document.toObject(Place::class.java)
                    callback(place)
                }
            }
            .addOnFailureListener { e ->
                Log.w("PlaceRepository", "Error getting documents.", e)
                callback(null)
            }
    }


    fun getCommentsForPlace(id: String): List<String>? {
        return placeCollection.document(id).get().getResult().get("comments") as? List<String>
    }

    fun addCommentForPlace(id: String, comment: String) {
        placeCollection.document(id).update("comments", FieldValue.arrayUnion(comment))
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("PlaceRepository", "Comment added successfully")
                } else {
                    Log.d("PlaceRepository", "Error adding comment: ${task.exception?.message}")
                }
            }
    }
    suspend fun getUserIdByEmail(email: String): String? {
        return try {
            val querySnapshot = userCollection.whereEqualTo("email", email).get().await()
            querySnapshot.documents.firstOrNull()?.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    fun updatePlaceRating(placeId: String, newRating: Float) {
        val placeRef = db.collection("places").document(placeId)

        placeRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val place = document.toObject(Place::class.java)
                place?.let {
                    val currentRating = it.rating ?: 0.0
                    val currentRatingNum = it.ratingNum ?: 0

                    val newRatingNum = currentRatingNum + 1
                    val newRatingSum = currentRating * currentRatingNum + newRating
                    val newAverageRating = newRatingSum / newRatingNum

                    placeRef.update(
                        mapOf(
                            "rating" to newAverageRating,
                            "ratingNum" to newRatingNum
                        )
                    ).addOnSuccessListener {
                        Log.d("PlaceRepository", "Rating updated successfully")
                    }.addOnFailureListener { e ->
                        Log.e("PlaceRepository", "Error updating rating", e)
                    }
                }
            } else {
                Log.e("PlaceRepository", "Document does not exist")
            }
        }.addOnFailureListener { e ->
            Log.e("PlaceRepository", "Error fetching place", e)
        }
    }



    fun likePlace(placeId: String) {
        val placeRef = db.collection("places").document(placeId)
        placeRef.update("likes", FieldValue.increment(1))
    }

    fun dislikePlace(placeId: String) {
        val placeRef = db.collection("places").document(placeId)
        placeRef.update("dislikes", FieldValue.increment(1))
    }

    suspend fun likeUserForPlace(placeId: String) {
        val place = getPlaceById(placeId).await().toObject(Place::class.java)
        if (place != null) {
            val ratingNum = place.ratingNum + 3
            val rating = place.rating + 0.3
            place.ratingNum = ratingNum
            place.rating = rating

            updateRatingForPlace(placeId, ratingNum, rating)

            val creatorEmail = place.creatorID
            if (creatorEmail != null) {
                if (creatorEmail.isNotBlank()) {
                    val userId = creatorEmail?.let { getUserIdByEmail(it) }
                    if (userId != null) {
                        val userDoc = userCollection.document(userId).get().await().toObject(User::class.java)
                        if (userDoc != null) {
                            userDoc.points += 3
                            updatePointsForUser(userId, userDoc.points)
                        } else {
                            Log.d("PlaceRepository", "User not found")
                        }
                    } else {
                        Log.d("PlaceRepository", "User ID not found")
                    }
                } else {
                    Log.d("PlaceRepository", "Creator email is blank")
                }
            }
        } else {
            Log.d("PlaceRepository", "Place not found")
        }
    }

    suspend fun dislikeUserForPlace(placeId: String) {
        val place = getPlaceById(placeId).await().toObject(Place::class.java)
        if (place != null) {
            val ratingNum = place.ratingNum - 2
            val rating = place.rating - 0.2
            place.ratingNum = ratingNum
            place.rating = rating

            updateRatingForPlace(placeId, ratingNum, rating)

            val creatorEmail = place.creatorID
            if (creatorEmail != null) {
                if (creatorEmail.isNotBlank()) {
                    val userId = creatorEmail?.let { getUserIdByEmail(it) }
                    if (userId != null) {
                        val userDoc = userCollection.document(userId).get().await().toObject(User::class.java)
                        if (userDoc != null) {
                            userDoc.points -= 2
                            updatePointsForUser(userId, userDoc.points)
                        } else {
                            Log.d("PlaceRepository", "User not found")
                        }
                    } else {
                        Log.d("PlaceRepository", "User ID not found")
                    }
                } else {
                    Log.d("PlaceRepository", "Creator email is blank")
                }
            }
        } else {
            Log.d("PlaceRepository", "Place not found")
        }
    }

    fun updateRatingForPlace(id: String, ratingNum: Int, rating: Double) {
        placeCollection.document(id).update("ratingNum", ratingNum, "rating", rating)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("PlaceRepository", "Rating updated successfully")
                } else {
                    Log.d("PlaceRepository", "Error updating rating: ${task.exception?.message}")
                    task.exception?.printStackTrace()
                }
            }
    }

    fun updatePointsForUser(userId: String, points: Int) {
        userCollection.document(userId).update("points", points)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("PlaceRepository", "Points updated successfully")
                } else {
                    Log.d("PlaceRepository", "Error updating points: ${task.exception?.message}")
                }
            }
    }

    fun getPlacesByCreator(creatorEmail: String, onSuccess: (List<Place>) -> Unit) {
        db.collection("places")
            .whereEqualTo("creatorID", creatorEmail)
            .get()
            .addOnSuccessListener { result ->
                val places = result.map { it.toObject(Place::class.java) }
                onSuccess(places)
            }
            .addOnFailureListener { e ->
                Log.e("PlaceRepository", "Error fetching places", e)
                onSuccess(emptyList())
            }
    }

    suspend fun getPlaces(typeFilter: String?): List<Place> {
        return try {
            val query = db.collection("places")

            // Primena filtera ako je tip naveden
            val filteredQuery = if (typeFilter != null && typeFilter != "Svi Tipovi") {
                query.whereEqualTo("type", typeFilter)
            } else {
                query
            }

            val result = filteredQuery.get().await()

            result.documents.mapNotNull { document ->
                document.toObject(Place::class.java)?.apply {
                    id = document.id // Postavljanje ID-a dokumenta
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }


}