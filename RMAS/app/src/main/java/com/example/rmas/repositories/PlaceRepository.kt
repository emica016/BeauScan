package com.example.rmas.data

import android.content.Context
import android.util.Log
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
        placeCollection.add(place).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("PlaceRepository", "Place saved successfully")
            } else {
                Log.d("PlaceRepository", "Error saving place: ${task.exception?.message}")
            }
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

    fun likeUserForPlace(id: String, userId: String) {
        getPlace(id) { place ->
            if (place != null) {
                val ratingNum = place.ratingNum + 3
                val rating = place.rating + 0.3
                place.ratingNum = ratingNum
                place.rating = rating

                updateRatingForPlace(id, ratingNum, rating)

                userCollection.document(userId).get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = task.result?.toObject(User::class.java)
                        if (user != null) {
                            user.points += 3
                            updatePointsForUser(userId, user.points)
                        } else {
                            Log.d("PlaceRepository", "User not found")
                        }
                    } else {
                        Log.d("PlaceRepository", "Error getting user: ${task.exception?.message}")
                    }
                }
            } else {
                Log.d("PlaceRepository", "Place not found")
            }
        }
    }

    fun dislikeUserForPlace(id: String, userId: String) {
        getPlace(id) { place ->
            if (place != null) {
                val ratingNum = place.ratingNum - 2
                val rating = place.rating - 0.2
                place.ratingNum = ratingNum
                place.rating = rating

                updateRatingForPlace(id, ratingNum, rating)

                userCollection.document(userId).get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = task.result?.toObject(User::class.java)
                        if (user != null) {
                            user.points -= 2
                            updatePointsForUser(userId, user.points)
                        } else {
                            Log.d("PlaceRepository", "User not found")
                        }
                    } else {
                        Log.d("PlaceRepository", "Error getting user: ${task.exception?.message}")
                    }
                }
            } else {
                Log.d("PlaceRepository", "Place not found")
            }
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


