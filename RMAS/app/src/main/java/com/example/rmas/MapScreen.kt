package com.example.rmas

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import coil.compose.rememberImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.example.rmas.data.Place
import com.example.rmas.data.PlaceRepository
import com.example.rmas.data.Response
import com.example.rmas.location.LocationService
import com.example.rmas.services.CameraService
import com.example.rmas.services.uploadImageToStorage
import com.google.accompanist.permissions.isGranted
import com.google.firebase.storage.FirebaseStorage
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(requestId: String?, placeId: String?, placeRespond: String?, navHostController: NavHostController) {
    val context = LocalContext.current
    val placeRepository = remember { PlaceRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    val placeMarkers = remember { mutableStateMapOf<String, Marker?>() }
    val currentLocation = remember { mutableStateOf<LatLng?>(null) }
    val places = remember { mutableStateListOf<Place>() }
    val selectedPlaceId = remember { mutableStateOf<String?>(null) }
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    var selectedType by remember { mutableStateOf<String?>(null) }
    var showAddPlaceDialog by remember { mutableStateOf<Pair<LatLng, Boolean>?>(null) }
    var showAddResponseDialog by remember { mutableStateOf<Pair<Place, Boolean>?>(null) }

    val mapProperties = remember { MapProperties(isMyLocationEnabled = true) }
    val mapUiSettings = remember { MapUiSettings(zoomControlsEnabled = true) }

    var currentLocationMarker by remember { mutableStateOf<Marker?>(null) }


    LaunchedEffect(selectedType) {
        if (googleMap != null) {
            fetchPlaces(placeRepository, places, placeMarkers, googleMap, placeId, selectedType, placeRespond)
        }
    }

    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    if (locationPermissionState.status.isGranted) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        val intent = Intent(context, LocationService::class.java).apply {
                            action = LocationService.ACTION_FIND_NEARBY
                        }
                        ContextCompat.startForegroundService(context, intent)
                    },
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                ) {
                    Text("Start Service")
                }

                Button(
                    onClick = {
                        val intent = Intent(context, LocationService::class.java).apply {
                            action = LocationService.ACTION_STOP
                        }
                        context.stopService(intent)
                    },
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                ) {
                    Text("Stop Service")
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        MapView(context).apply {
                            onCreate(Bundle())
                            getMapAsync { map ->
                                googleMap = map
                                map.isMyLocationEnabled = true
                                map.uiSettings.isZoomControlsEnabled = true

                                map.setOnMapLoadedCallback {
                                    coroutineScope.launch {
                                        fetchPlaces(placeRepository, places, placeMarkers, googleMap, placeId, selectedType, placeRespond)
                                    }
                                }
                                map.setOnMapClickListener { latLng ->
                                    currentLocation.value = latLng
                                    showAddPlaceDialog = latLng to true
                                }
                                map.setOnMarkerClickListener { marker ->
                                    marker.title?.let { title ->
                                        val place = places.find { it.name == title }
                                        selectedPlaceId.value = place?.id
                                    }
                                    true
                                }
                            }
                        }
                    }
                )
            }

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {


                FilterChip(
                    selected = selectedType == null,
                    onClick = { selectedType = null },
                    label = { Text("Svi Tipovi") }
                )
                FilterChip(
                    selected = selectedType == "Frizerski salon",
                    onClick = { selectedType = "Frizerski salon" },
                    label = { Text("Frizerski salon") }
                )
                FilterChip(
                    selected = selectedType == "Kozmeticki salon",
                    onClick = { selectedType = "Kozmeticki salon" },
                    label = { Text("Kozmeticki salon") }
                )
                FilterChip(
                    selected = selectedType == "Parfimerija",
                    onClick = { selectedType = "Parfimerija" },
                    label = { Text("Parfimerija") }
                )
                FilterChip(
                    selected = selectedType == "Drogerija",
                    onClick = { selectedType = "Drogerija" },
                    label = { Text("Drogerija") }
                )
            }


        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val latLng = LatLng(it.latitude, it.longitude)
                        currentLocation.value = latLng
                        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                        googleMap?.let { map ->
                            currentLocationMarker?.remove()
                            currentLocationMarker = map.addMarker(
                                MarkerOptions()
                                    .position(latLng)
                                    .title("Moja Lokacija")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                            )
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Lokaciona dozvola je potrebna za korišćenje ove funkcije")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                Text("Zatraži Dozvolu")
            }
        }
    }

    // Show AddPlaceDialog
    showAddPlaceDialog?.let { (latLng, showDialog) ->
        if (showDialog) {
            ShowAddPlaceDialog(
                latLng = latLng,
                placeRepository = placeRepository,
                coroutineScope = coroutineScope,
                onPlaceSaved = { place ->
                    coroutineScope.launch {
                        fetchPlaces(placeRepository, places, placeMarkers, googleMap, placeId, selectedType, placeRespond)
                    }
                    showAddPlaceDialog = null
                    if (requestId != null) {
                        // If requestId is not null, show the AddResponseDialog
                        showAddResponseDialog = place to true
                    }
                },
                onDismiss = { showAddPlaceDialog = null }
            )
        }
    }

    // Show AddResponseDialog
    showAddResponseDialog?.let { (place, showDialog) ->
        if (showDialog) {
            ShowAddResponseDialog(
                place = place,
                requestId = requestId,
                placeRepository = placeRepository,
                coroutineScope = coroutineScope,
                onDismiss = {
                    showAddResponseDialog = null
                }
            )
        }
    }

    selectedPlaceId.value?.let { placeId ->
        currentLocation.value?.let {
            showPlaceDetails(placeId, placeRepository, it) {
                selectedPlaceId.value = null
            }
        }
    }
}

private fun fetchPlaces(
    placeRepository: PlaceRepository,
    places: SnapshotStateList<Place>,
    placeMarkers: MutableMap<String, Marker?>,
    googleMap: GoogleMap?,
    placeId: String?,
    selectedType: String?,
    placeRespond: String?
) {
    placeRepository.getAllPlaces().get().addOnSuccessListener { result ->
        places.clear()
        placeMarkers.values.forEach { it?.remove() }
        placeMarkers.clear()

        for (document in result) {
            val place = document.toObject(Place::class.java)
            if (selectedType == null || place.type == selectedType) {
                places.add(place)
                val position = LatLng(place.location.latitude, place.location.longitude)
                val markerOptions = MarkerOptions()
                    .position(position)
                    .title(place.name)
                    .icon(
                        if (place.id == placeId || (placeRespond!= null && place.id == placeRespond )) {
                            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN) // Green for selected place
                        } else {
                            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED) // Default color for other places
                        }

                    )

                val marker = googleMap?.addMarker(markerOptions)
                placeMarkers[place.id] = marker
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowAddPlaceDialog(
    latLng: LatLng,
    placeRepository: PlaceRepository,
    coroutineScope: CoroutineScope,
    onPlaceSaved: (Place) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val cameraService = remember { CameraService(context) }
    var placeName by remember { mutableStateOf("") }
    val listOfTypes = listOf("Kozmeticki salon", "Frizerski salon", "Drogerija", "Parfimerija")
    var type by remember { mutableStateOf(listOfTypes[0]) }

    val listOfPurposes = listOf("Tretman", "Usluga", "Kupovina", "Uzorkovanje proizvoda")
    var purpose by remember { mutableStateOf(listOfPurposes[0]) }
    var expanded by remember { mutableStateOf(false) }
    var expandedPur by remember { mutableStateOf(false) }

    var placeDescription by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf<String?>("") }

    // Getting the current user
    val currentUser = FirebaseAuth.getInstance().currentUser?.email ?: "Anonymous"

    // Set up the CameraService
    cameraService.Setup { uri ->
        coroutineScope.launch {
            imageUri = uri
            imageUrl = uploadImageToStorage(uri, context)
        }
        0 // Return some int as callback expects Int
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj Novo Mesto") },
        text = {
            Column {
                OutlinedTextField(
                    value = placeName,
                    onValueChange = { placeName = it },
                    label = { Text("Naziv Mesta") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = false,
                        value = type,
                        onValueChange = {},
                        label = { Text("Tip") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOfTypes.forEach { selectedOption ->
                            DropdownMenuItem(
                                text = { Text(selectedOption) },
                                onClick = {
                                    type = selectedOption
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expandedPur,
                    onExpandedChange = { expandedPur = !expandedPur }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = false,
                        value = purpose,
                        onValueChange = {},
                        label = { Text("Delatnost") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPur) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPur,
                        onDismissRequest = { expandedPur = false }
                    ) {
                        listOfPurposes.forEach { selectedOptionPurpose ->
                            DropdownMenuItem(
                                text = { Text(selectedOptionPurpose) },
                                onClick = {
                                    purpose = selectedOptionPurpose
                                    expandedPur = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = placeDescription,
                    onValueChange = { placeDescription = it },
                    label = { Text("Opis Mesta") }
                )
                Spacer(modifier = Modifier.height(8.dp))

                imageUri?.let {
                    Image(
                        painter = rememberImagePainter(it),
                        contentDescription = null,
                        modifier = Modifier
                            .size(150.dp)
                            .border(2.dp, Color.Gray, RectangleShape)
                            .padding(4.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    Button(onClick = { cameraService.takePicture() }) {
                        Text("Snimite sliku")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { cameraService.uploadPicture() }) {
                        Text("Izaberite iz galerije")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                coroutineScope.launch {
                    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val currentDate = Date()
                    val place = Place(
                        id = UUID.randomUUID().toString(),
                        name = placeName,
                        location = GeoPoint(latLng.latitude, latLng.longitude),
                        type = type,
                        purpose = purpose,
                        description = placeDescription,
                        creatorID = currentUser,
                        dateCreated = dateFormatter.format(currentDate),
                        timeCreated = timeFormatter.format(currentDate),
                        imageUrl = imageUrl
                    )
                    placeRepository.savePlace(place)
                    onPlaceSaved(place)
                }
            }) {
                Text("Sačuvaj")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Odustani")
            }
        }
    )
}

/*private fun createImageUri(context: Context): Uri? {
    return try {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "temp_image_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    } catch (e: Exception) {
        Log.e("ImageCapture", "Failed to create image URI", e)
        null
    }
}

private suspend fun uploadImageToStorage(uri: Uri, context: Context): String? {
    return try {
        val storageReference = FirebaseStorage.getInstance().reference.child("images/${UUID.randomUUID()}")
        storageReference.putFile(uri).await()
        storageReference.downloadUrl.await().toString()
    } catch (e: Exception) {
        Log.e("ImageUpload", "Failed to upload image to storage", e)
        null
    }
}
*/

@Composable
fun ShowAddResponseDialog(
    place: Place,
    requestId: String?,
    placeRepository: PlaceRepository,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit
) {
    var comment by remember { mutableStateOf("") }
    val currentUser = FirebaseAuth.getInstance().currentUser?.email ?: "Anonymous"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj Komentar") },
        text = {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Komentar") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                coroutineScope.launch {
                    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val currentDate = Date()
                    val response = Response(
                        placeId = place.id,
                        creatorID = currentUser,
                        date = dateFormatter.format(currentDate),
                        time = timeFormatter.format(currentDate),
                        comment = comment,
                        requestId = requestId
                    )
                    placeRepository.saveResponse(response)
                    onDismiss()
                }
            }) {
                Text("Sačuvaj")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Odustani")
            }
        }
    )
}

@Composable
fun showPlaceDetails(
    placeId: String,
    placeRepository: PlaceRepository,
    userLocation: LatLng, // Trenutna lokacija korisnika
    onDismiss: () -> Unit
) {
    var place by remember { mutableStateOf<Place?>(null) }
    var rating by remember { mutableStateOf(0f) }
    var distance by remember { mutableStateOf(0f) } // Udaljenost u kilometrima
    var snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(placeId) {
        // Preuzmi informacije o mestu
        val placeDocument = placeRepository.getPlaceById(placeId).await()
        place = placeDocument.toObject(Place::class.java)

        // Ako mesto postoji, izračunaj udaljenost
        place?.let {
            val placeLocation = LatLng(it.location.latitude, it.location.longitude)
            distance = calculateDistance(userLocation, placeLocation)
            rating = (it.rating ?: 0f).toFloat()
        }
    }

    place?.let {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = "Detalji o Mestu", style = MaterialTheme.typography.titleLarge)
            },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("Zatvori")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    // Ime mesta
                    Text(
                        text = it.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Tip, svrha, opis
                    Text(text = "Tip: ${it.type}", style = MaterialTheme.typography.bodyLarge)
                    Text(text = "Svrha: ${it.purpose}", style = MaterialTheme.typography.bodyLarge)
                    Text(text = "Opis: ${it.description}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Udaljenost
                    Text(text = "Udaljenost: ${"%.2f".format(distance)} km", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Kreator
                    Text(text = "Kreator: ${it.creatorID}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Lokacija
                    Text(text = "Lokacija: ${it.location.latitude}, ${it.location.longitude}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Slika mesta
                    it.imageUrl?.let { imageUrl ->
                        Image(
                            painter = rememberImagePainter(data = imageUrl),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Prikaz ocene
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(text = "Ocena: ", modifier = Modifier.padding(end = 8.dp))
                        RatingBar(
                            rating = rating,
                            onRatingChanged = { newRating ->
                                rating = newRating
                                coroutineScope.launch {
                                    placeRepository.updatePlaceRating(it.id, newRating)
                                }
                            }
                        )
                    }

                    // Dugmad za Like i Dislike
                    Row(
                        modifier = Modifier.padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    placeRepository.likeUserForPlace(it.id)
                                    placeRepository.likePlace(it.id)
                                    snackbarHostState.showSnackbar("Mesto lajkovano")
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_thumb_up_24),
                                contentDescription = "Like",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    placeRepository.dislikeUserForPlace(it.id)
                                    placeRepository.dislikePlace(it.id)
                                    snackbarHostState.showSnackbar("Mesto dislajkovano")
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_thumb_down_24),
                                contentDescription = "Dislike",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        )

        // Prikaz Snackbara
        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
fun RatingBar(
    rating: Float,
    onRatingChanged: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..5) {
            val icon: Painter = if (i <= rating) {
                painterResource(id = R.drawable.baseline_star_24) // Use an image resource for filled star
            } else {
                painterResource(id = R.drawable.baseline_star_outline_24) // Use an image resource for outlined star
            }

            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onRatingChanged(i.toFloat()) }
                    .padding(2.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

fun calculateDistance(location1: LatLng, location2: LatLng): Float {
    val results = FloatArray(1)
    Location.distanceBetween(
        location1.latitude, location1.longitude,
        location2.latitude, location2.longitude,
        results
    )
    return results[0] / 1000 // Konvertuj iz metara u kilometre
}
