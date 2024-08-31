package com.example.rmas

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.rmas.data.Request
import com.example.rmas.database.FirebaseDatabase
import com.example.rmas.navigation.Screens
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun RequestDetails(
    userId: String,
    requestId: String,
    navHostController: NavHostController,
    onResponseClicked: (String) -> Unit
) {
    val scrolState = rememberScrollState()
    var request by remember { mutableStateOf<Request?>(null) }
    var numberOfResponses by remember { mutableStateOf(0) }
    var responses by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(requestId) {
        FirebaseFirestore.getInstance().collection("requests").document(requestId).get().addOnSuccessListener { snapshot ->
            request = snapshot.toObject(Request::class.java)
            numberOfResponses = request?.numberOfResponses ?: 0
            responses = request?.responses ?: emptyList()
        }.await()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        BoxWithConstraints {
            Surface {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrolState)
                ) {
                    request?.let { req ->
                        ProfilHeader(req, numberOfResponses, responses, navHostController, requestId, onResponseClicked)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilHeader(
    request: Request,
    numberOfResponses: Int,
    responses: List<String>,
    navHostController: NavHostController,
    requestId: String,
    onResponseClicked: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(25.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
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
                    .heightIn(max = 300.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                painter = painterResource(id = imageId),
                contentDescription = null
            )
        } ?: Text("Nemam sliku za ovaj tip")

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.wrapContentSize(),
                color = Color(0xFFE2756D)
            ) {
                Text(
                    text = request.type,
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.padding(4.dp))
            Text(text = request.date, fontSize = 14.sp, textAlign = TextAlign.End)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.wrapContentSize(),
                color = Color(0xFF95C9C4)
            ) {
                Text(text = request.purpose, fontSize = 16.sp)
            }
            Surface(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.wrapContentSize(),
                color = Color(0xFFC586CF)
            ) {
                Text(text = request.creatorID, fontSize = 16.sp)
            }
        }

        Text(text = request.description, fontSize = 18.sp)

        Surface(
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.wrapContentSize(),
            color = Color(0xFF7EA9CC)
        ) {
            Text(
                text = "Broj odgovora: $numberOfResponses",
                fontSize = 14.sp
            )
        }

        if (numberOfResponses > 0) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(responses.size) { index ->
                    Text(text = responses[index], fontSize = 16.sp)
                }
            }
        } else {
            Text(text = "Jos uvek nema odgovora.")
        }

        Button(
            onClick = {
                // Navigate to MapScreen with requestId
                onResponseClicked(requestId)
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Odgovori")
        }
    }
}
