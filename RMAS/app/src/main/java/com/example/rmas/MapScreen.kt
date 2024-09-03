package com.example.rmas

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
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
import com.example.rmas.data.Request
import com.example.rmas.data.Response
import com.example.rmas.services.uploadImageToStorage
import com.google.accompanist.permissions.isGranted
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(requestId: String?, placeId: String?, navHostController: NavHostController) {
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
            fetchPlaces(placeRepository, places, placeMarkers, googleMap, placeId, selectedType)
        }
    }

    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    if (locationPermissionState.status.isGranted) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                                        fetchPlaces(placeRepository, places, placeMarkers, googleMap, placeId, selectedType)
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
                        fetchPlaces(placeRepository, places, placeMarkers, googleMap, placeId, selectedType)
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
        showPlaceDetails(placeId, placeRepository) {
            selectedPlaceId.value = null
        }
    }
}

private fun fetchPlaces(
    placeRepository: PlaceRepository,
    places: SnapshotStateList<Place>,
    placeMarkers: MutableMap<String, Marker?>,
    googleMap: GoogleMap?,
    placeId: String?,
    selectedType: String?
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
                        if (place.id == placeId) {
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
    var context = LocalContext.current
    var placeName by remember { mutableStateOf("") }
    var placeType by remember { mutableStateOf("Frizerski salon") }
    var placePurpose by remember { mutableStateOf("Usluga") }
    var placeDescription by remember { mutableStateOf("") }
    var expandedType by remember { mutableStateOf(false) }
    var expandedPurpose by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf<String?>(null) }

    // Getting the current user
    val currentUser = FirebaseAuth.getInstance().currentUser?.email ?: "Anonymous"

    // Image capture and selection
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            coroutineScope.launch {
                imageUri?.let {
                    // Handle the imageUri (upload or display)
                    imageUrl = uploadImageToStorage(it, context)
                }
            }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        coroutineScope.launch {
            uri?.let {
                imageUri = it
                // Handle the imageUri (upload or display)
                imageUrl = uploadImageToStorage(it, context)
            }
        }
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
                    expanded = expandedType,
                    onExpandedChange = { expandedType = !expandedType }
                ) {
                    OutlinedTextField(
                        value = placeType,
                        onValueChange = {},
                        label = { Text("Tip Mesta") },
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Frizerski salon", "Kozmeticki salon", "Parfimerija", "Drogerija").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    placeType = type
                                    expandedType = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedPurpose,
                    onExpandedChange = { expandedPurpose = !expandedPurpose }
                ) {
                    OutlinedTextField(
                        value = placePurpose,
                        onValueChange = {},
                        label = { Text("Svrha Mesta") },
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPurpose)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPurpose,
                        onDismissRequest = { expandedPurpose = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Usluga", "Kupovina", "Tretman", "Uzorkovanje proizvoda").forEach { purpose ->
                            DropdownMenuItem(
                                text = { Text(purpose) },
                                onClick = {
                                    placePurpose = purpose
                                    expandedPurpose = false
                                }
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
                    Button(onClick = {
                        val photoUri = createImageUri(context)
                        photoUri?.let {
                            imageUri = it
                            takePictureLauncher.launch(photoUri)
                        }
                    }) {
                        Text("Snimite sliku")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { pickImageLauncher.launch("image/*") }) {
                        Text("Izaberite iz galerije")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                coroutineScope.launch {
                    imageUri?.let { uri ->
                        imageUrl = uploadImageToStorage(uri, context)
                    }
                    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val currentDate = Date()
                    val place = Place(
                        id = UUID.randomUUID().toString(),
                        name = placeName,
                        location = GeoPoint(latLng.latitude, latLng.longitude),
                        type = placeType,
                        purpose = placePurpose,
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


private fun createImageUri(context: Context): Uri? {
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
private fun showPlaceDetails(placeId: String, placeRepository: PlaceRepository, onDismiss: () -> Unit) {
    val place = remember { mutableStateOf<Place?>(null) }

    LaunchedEffect(placeId) {
        placeRepository.getPlace(placeId) { fetchedPlace ->
            place.value = fetchedPlace
        }
    }

    place.value?.let {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text(it.name) },
            text = {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    it.imageUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .size(150.dp)
                                .border(2.dp, Color.Gray, RectangleShape)
                                .padding(4.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text("Tip: ${it.type}")
                    Text("Svrha: ${it.purpose}")
                    Text("Opis: ${it.description}")
                    Text("Lokacija: ${it.location.latitude}, ${it.location.longitude}")
                    Text("Kreator: ${it.creatorID}")
                    Text("Datum Kreiranja: ${it.dateCreated}")
                    Text("Vreme Kreiranja: ${it.timeCreated}")
                }
            },
            confirmButton = {
                Button(onClick = { onDismiss() }) {
                    Text("OK")
                }
            }
        )
    }
}