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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
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
            .background(Color(0xFFE0E7FF)) // Change to your preferred background color
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "Leaderboard",
                style = TextStyle(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            contentColor = Color.White,
            modifier = Modifier.padding(8.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .background(
                            if (selectedTab == index) MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                            else Color.Transparent
                        )
                        .clip(MaterialTheme.shapes.small)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp)) // Added space between tab row and content

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
            val responseCount = responseCollection.documents.count { it.getString("creatorID") == user.email }
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
        val medalResId = when (rank) {
            1 -> R.drawable.gold_medal
            2 -> R.drawable.silver_medal
            3 -> R.drawable.bronze_medal
            else -> null
        }

        medalResId?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = "Medal",
                modifier = Modifier.size(30.dp)
            )
        } ?: Text(
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
    // Custom divider
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)) // Customize color and transparency
    )
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
        val medalResId = when (rank) {
            1 -> R.drawable.gold_medal
            2 -> R.drawable.silver_medal
            3 -> R.drawable.bronze_medal
            else -> null
        }

        medalResId?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = "Medal",
                modifier = Modifier.size(30.dp)
            )
        } ?: Text(
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
    // Custom divider
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)) // Customize color and transparency
    )
}
