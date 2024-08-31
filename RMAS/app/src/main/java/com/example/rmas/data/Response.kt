package com.example.rmas.data

import com.google.firebase.firestore.DocumentId


data class Response(
    @DocumentId val id: String = "",
    val placeId: String = "",
    val creatorID: String = "",
    val date: String = "",
    val time: String = "",
    val comment: String = ""
)
