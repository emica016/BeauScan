package com.example.rmas

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.rmas.data.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

@Composable
fun LeaderboardScreen(navController: NavHostController) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Users", "Places")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
            .padding(vertical = 12.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank.",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.width(40.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Image(
            painter = rememberAsyncImagePainter(user.image),
            contentDescription = "User Image",
            modifier = Modifier
                .size(50.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = user.username,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Points: ${user.points}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "Requests: $requestCount",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "Responses: $responseCount",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
    Divider(color = Color.LightGray, thickness = 1.dp)
}

@Composable
fun PlaceLeaderboardTab() {
    val places = remember { mutableStateListOf<Place>() }

    LaunchedEffect(Unit) {
        val placeCollection = Firebase.firestore.collection("places").get().await()
        val placesList = placeCollection.toObjects(Place::class.java)
        places.addAll(placesList)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
            .padding(vertical = 12.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank.",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.width(40.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        place.imageUrl?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = "Place Image",
                modifier = Modifier
                    .size(50.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = place.name,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Type: ${place.type}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "Purpose: ${place.purpose}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "Location: ${place.location.latitude}, ${place.location.longitude}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "Rating: ${place.rating}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
    Divider(color = Color.LightGray, thickness = 1.dp)
}
