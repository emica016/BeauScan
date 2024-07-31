package com.example.rmas.data

class Request(
    val id: String = "",
    val type: String = "",
    val purpose: String = "",
    val description: String = "",
    val creatorID: String = "",
    val date : String = "",
    val numberOfResponses: Int = 0,
    val responses: List<String> = emptyList()
)