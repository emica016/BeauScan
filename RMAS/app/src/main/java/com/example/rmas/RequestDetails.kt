package com.example.rmas

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavHostController
import com.example.rmas.data.Request
import com.example.rmas.data.Response
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RequestDetails(
    userId: String,
    requestId: String?,
    navHostController: NavHostController,
    onResponseClicked: (String) -> Unit,
    onPlaceRespond: (String) -> Unit
) {
    var request by remember { mutableStateOf<Request?>(null) }
    var responses by remember { mutableStateOf<List<Response>>(emptyList()) }

    LaunchedEffect(requestId) {
        if (requestId != null) {
            val db = FirebaseFirestore.getInstance()

            // Fetching the request details
            val requestSnapshot = db.collection("requests")
                .document(requestId)
                .get()
                .await()
            request = requestSnapshot.toObject(Request::class.java)

            // Fetching responses based on requestId
            val responseSnapshot = db.collection("responses")
                .whereEqualTo("requestId", requestId)
                .get()
                .await()
            responses = responseSnapshot.toObjects(Response::class.java)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        request?.let { req ->
            if (requestId != null) {
                ProfileHeader(
                    request = req,
                    responses = responses,
                    navHostController = navHostController,
                    requestId = requestId,
                    onResponseClicked = onResponseClicked,
                    onPlaceRespond = onPlaceRespond
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    request: Request,
    responses: List<Response>,
    navHostController: NavHostController,
    requestId: String,
    onResponseClicked: (String) -> Unit,
    onPlaceRespond: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val typeImage = when (request.type) {
            "Kozmeticki salon" -> R.drawable.beautysalon
            "Frizerski salon" -> R.drawable.hairsalon
            "Drogerija" -> R.drawable.store
            "Parfimerija" -> R.drawable.parfume
            else -> null
        }

        typeImage?.let { imageId ->
            Image(
                modifier = Modifier
                    .heightIn(max = 250.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                painter = painterResource(id = imageId),
                contentDescription = null
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.wrapContentSize(),
                color = Color(0xFFE2756D)
            ) {
                Text(
                    text = request.type,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(8.dp),
                    color = Color.White
                )
            }

            Text(
                text = request.date,
                fontSize = 14.sp,
                textAlign = TextAlign.End,
                color = Color.Gray
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.wrapContentSize(),
                color = Color(0xFF95C9C4)
            ) {
                Text(
                    text = request.purpose,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(8.dp),
                    color = Color.White
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.wrapContentSize(),
                color = Color(0xFFC586CF)
            ) {
                Text(
                    text = request.creatorID,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(8.dp),
                    color = Color.White
                )
            }
        }

        Text(
            text = request.description,
            fontSize = 18.sp,
            modifier = Modifier.padding(vertical = 8.dp),
            color = Color.DarkGray
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.wrapContentSize(),
                color = Color(0xFF7EA9CC)
            ) {
                Text(
                    text = "Broj odgovora: ${responses.size}",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(8.dp),
                    color = Color.White
                )
            }

            Button(
                onClick = { onResponseClicked(requestId) },
                modifier = Modifier
                    .wrapContentSize()
                    .padding(start = 8.dp) // Adjust padding if necessary
            ) {
                Text("Odgovori", fontSize = 16.sp)
            }
        }

        if (responses.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(responses) { response ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Kreator: ${response.creatorID}",
                                fontSize = 16.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "Komentar:",
                                fontSize = 16.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                text = response.comment,
                                fontSize = 16.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Datum: ${response.date}",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Pogledaj na mapi",
                                    fontSize = 14.sp,
                                    color = Color.Blue,
                                    textDecoration = TextDecoration.LineThrough,
                                    modifier = Modifier.clickable {
                                        onPlaceRespond(response.placeId ?: "")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                text = "Još uvek nema odgovora.",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}
