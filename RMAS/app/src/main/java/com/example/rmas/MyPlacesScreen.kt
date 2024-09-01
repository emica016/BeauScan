package com.example.rmas

import androidx.compose.ui.platform.LocalContext
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.rmas.data.Place
import com.example.rmas.data.PlaceRepository
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPlacesScreen(navHostController: NavHostController, onPlaceClick: (String) -> Unit) {
    val context = LocalContext.current
    val placeRepository = remember { PlaceRepository(context) }
    val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
    val places = remember { mutableStateListOf<Place>() }

    // Fetch places created by the current user
    LaunchedEffect(currentUserEmail) {
        currentUserEmail?.let { email ->
            placeRepository.getPlacesByCreator(email) { fetchedPlaces ->
                places.clear()
                places.addAll(fetchedPlaces)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Places") }
            )
        }
    ) { paddingValues ->
        if (places.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("You haven't added any places yet.")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                places.forEach { place ->
                    PlaceItem(place = place, onPlaceClick = {
                        onPlaceClick(place.id)
                    })
                }
            }
        }
    }
}

@Composable
fun PlaceItem(place: Place, onPlaceClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onPlaceClick(place.id) },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            place.imageUrl?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = place.name,
                    modifier = Modifier.size(80.dp)
                )
            }
            Column {
                Text(text = place.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = place.description ?: "No description",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
