package com.example.rmas

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberImagePainter
import com.example.rmas.data.*
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

@Composable
fun LeaderboardScreen(navController: NavHostController) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Users", "Places")

    Column {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        when (selectedTab) {
            0 -> UserLeaderboardTab()
            1 -> PlaceLeaderboardTab()
        }
    }
}

@Composable
fun UserLeaderboardTab() {
    // Get users and requests/responses from Firestore
    val users = remember { mutableStateListOf<User>() }
    val userRequestResponseCounts = remember { mutableStateMapOf<String, Pair<Int, Int>>() }

    LaunchedEffect(Unit) {
        val userCollection = Firebase.firestore.collection("users").get().await()
        val requestCollection = Firebase.firestore.collection("requests").get().await()
        val responseCollection = Firebase.firestore.collection("responses").get().await()

        val usersList = userCollection.toObjects(User::class.java)
        users.addAll(usersList)

        users.forEach { user ->
            val requestCount = requestCollection.documents.count { it.getString("creatorID") == user.username }
            val responseCount = responseCollection.documents.count { it.getString("creatorID") == user.username }
            userRequestResponseCounts[user.email] = Pair(requestCount, responseCount)
        }
    }

    LazyColumn {
        itemsIndexed(users.sortedByDescending { it.points }) { index, user ->
            val requestResponseCount = userRequestResponseCounts[user.email] ?: Pair(0, 0)
            UserLeaderboardItem(user, requestResponseCount.first, requestResponseCount.second, index + 1)
        }
    }
}

@Composable
fun UserLeaderboardItem(user: User, requestCount: Int, responseCount: Int, rank: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank.",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.width(30.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Image(
            painter = rememberImagePainter(data = user.image),
            contentDescription = "User Image",
            modifier = Modifier.size(50.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = user.username, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = "Points: ${user.points}")
            Text(text = "Requests: $requestCount")
            Text(text = "Responses: $responseCount")
        }
    }
    Divider(color = Color.Gray, thickness = 1.dp)
}

@Composable
fun PlaceLeaderboardTab() {
    // Get places from Firestore
    val places = remember { mutableStateListOf<Place>() }

    LaunchedEffect(Unit) {
        val placeCollection = Firebase.firestore.collection("places").get().await()
        val placesList = placeCollection.toObjects(Place::class.java)
        places.addAll(placesList)
    }

    LazyColumn {
        itemsIndexed(places.sortedByDescending { it.rating }) { index, place ->
            PlaceLeaderboardItem(place, index + 1)
        }
    }
}

@Composable
fun PlaceLeaderboardItem(place: Place, rank: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank.",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.width(30.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        place.imageUrl?.let {
            Image(
                painter = rememberImagePainter(data = it),
                contentDescription = "Place Image",
                modifier = Modifier.size(50.dp),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = place.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = "Type: ${place.type}")
            Text(text = "Purpose: ${place.purpose}")
            Text(text = "Location: ${place.location.latitude}, ${place.location.longitude}")
            Text(text = "Rating: ${place.rating}")
        }
    }
    Divider(color = Color.Gray, thickness = 1.dp)
}
