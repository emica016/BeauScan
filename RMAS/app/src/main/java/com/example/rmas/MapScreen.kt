package com.example.rmas

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import com.example.rmas.data.Place
import com.example.rmas.data.PlaceRepository
import com.google.accompanist.permissions.isGranted
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TREBA DA SE CUVA placeId u response, DA SE PODESI KAMERA
@OptIn(ExperimentalPermissionsApi::class)
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

    val mapProperties = remember { MapProperties(isMyLocationEnabled = true) }
    val mapUiSettings = remember { MapUiSettings(zoomControlsEnabled = true) }

    var showAddPlaceDialog by remember { mutableStateOf<Pair<LatLng, Boolean>?>(null) }
    var showAddResponseDialog by remember { mutableStateOf<Pair<Place?, Boolean>?>(null) }

    // Marker za trenutnu lokaciju
    var currentLocationMarker by remember { mutableStateOf<Marker?>(null) }

    // Request location permission
    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    if (locationPermissionState.status.isGranted) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                MapView(context).apply {
                    onCreate(Bundle())
                    getMapAsync { map ->
                        googleMap = map
                        map.isMyLocationEnabled = true
                        map.uiSettings.isZoomControlsEnabled = true

                        // Postavljanje plavog markera za trenutnu lokaciju
                        currentLocation.value?.let { location ->
                            // Ako marker već postoji, ukloni ga
                            currentLocationMarker?.remove()

                            // Dodaj novi marker sa plavom bojom
                            currentLocationMarker = map.addMarker(
                                MarkerOptions()
                                    .position(location)
                                    .title("Moja Lokacija")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                            )
                        }

                        map.setOnMapLoadedCallback {
                            coroutineScope.launch {
                                fetchPlaces(placeRepository, places, placeMarkers, googleMap, placeId)
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
            },
            update = { mapView ->
                mapView.getMapAsync { map ->
                    googleMap = map
                    currentLocation.value?.let {
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))

                        // Ako marker već postoji, ukloni ga
                        currentLocationMarker?.remove()

                        // Dodaj novi marker sa plavom bojom
                        currentLocationMarker = map.addMarker(
                            MarkerOptions()
                                .position(it)
                                .title("Moja Lokacija")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                    }
                }
            }
        )

        // Obtain current location
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val latLng = LatLng(it.latitude, it.longitude)
                        currentLocation.value = latLng
                        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                        // Dodavanje plavog markera za trenutnu lokaciju
                        googleMap?.let { map ->
                            // Ako marker već postoji, ukloni ga
                            currentLocationMarker?.remove()

                            // Dodaj novi marker sa plavom bojom
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

    // Prikazivanje dijaloga
    showAddPlaceDialog?.let { (latLng, showDialog) ->
        if (showDialog) {
            ShowAddPlaceDialog(
                latLng = latLng,
                placeRepository = placeRepository,
                coroutineScope = coroutineScope,
                onPlaceSaved = { place ->
                    if (requestId != null) {
                        // Prikazivanje dijaloga za dodavanje odgovora
                        showAddResponseDialog = place to true
                    } else {
                        // Osvježavanje mesta i zatvaranje dijaloga
                        coroutineScope.launch {
                            fetchPlaces(placeRepository, places, placeMarkers, googleMap, placeId)
                        }
                        showAddPlaceDialog = null
                    }
                },
                onDismiss = { showAddPlaceDialog = null }
            )
        }
    }

    showAddResponseDialog?.let { (place, showDialog) ->
        if (showDialog) {
            AddResponseDialog(
                place = place,
                requestId = requestId,
                placeRepository = placeRepository,
                onResponseSaved = {
                    coroutineScope.launch {
                        fetchPlaces(placeRepository, places, placeMarkers, googleMap, placeId)
                    }
                    showAddResponseDialog = null
                    showAddPlaceDialog = null // Zatvaranje dijaloga za dodavanje mesta takođe
                },
                onDismiss = {
                    showAddResponseDialog = null
                    showAddPlaceDialog = null // Zatvaranje dijaloga za dodavanje mesta takođe
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
    selectedPlaceId: String?
) {
    placeRepository.getAllPlaces().get().addOnSuccessListener { result ->
        places.clear()
        placeMarkers.values.forEach { it?.remove() }
        placeMarkers.clear()

        for (document in result) {
            val place = document.toObject(Place::class.java)
            places.add(place)
            val position = LatLng(place.location.latitude, place.location.longitude)
            val markerOptions = MarkerOptions().position(position).title(place.name)

            // Ako je ID mesta isti kao prosleđeni placeId, postavi marker u zelenoj boji
            val markerColor = if (place.id == selectedPlaceId) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_RED
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(markerColor))

            val marker = googleMap?.addMarker(markerOptions)
            placeMarkers[place.id] = marker
        }
    }.addOnFailureListener { exception ->
        Log.e("MapScreen", "Error fetching places: ${exception.message}")
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
    var placeName by remember { mutableStateOf("") }
    var placeType by remember { mutableStateOf("") }
    var placePurpose by remember { mutableStateOf("") }
    var placeDescription by remember { mutableStateOf("") }
    val currentUser = FirebaseAuth.getInstance().currentUser?.email ?: "Anonymous"
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
        }
    }

    val cameraUri = remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraUri.value?.let { uri ->
                imageUri = uri
                Log.e("CAMERA", "Image URI: $uri")
            }
        }
    }

    // Kreiranje privremene URI za kameru
    val createImageUri: () -> Uri = {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "temp_image.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
    }

    AlertDialog(
        onDismissRequest = {
            onDismiss()
        },
        title = { Text("Dodaj Mesto") },
        text = {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pregled slike
                Surface(
                    modifier = Modifier
                        .size(150.dp)
                        .border(3.dp, Color.White, RectangleShape),
                    color = Color.Gray
                ) {
                    imageUri?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Odabir slike
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_gallery),
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .clickable { imagePickerLauncher.launch("image/*") }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_camera),
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .clickable {
                                cameraUri.value = createImageUri()
                                cameraUri.value?.let { uri ->
                                    takePictureLauncher.launch(uri)
                                }
                            }
                    )
                }
                // Unos podataka o mestu
                OutlinedTextField(
                    value = placeName,
                    onValueChange = { placeName = it },
                    label = { Text("Naziv Mesta") }
                )
                OutlinedTextField(
                    value = placeType,
                    onValueChange = { placeType = it },
                    label = { Text("Tip Mesta") }
                )
                OutlinedTextField(
                    value = placePurpose,
                    onValueChange = { placePurpose = it },
                    label = { Text("Svrha Mesta") }
                )
                OutlinedTextField(
                    value = placeDescription,
                    onValueChange = { placeDescription = it },
                    label = { Text("Opis Mesta") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val currentDate = Date()
                val place = Place(
                    name = placeName,
                    type = placeType,
                    purpose = placePurpose,
                    description = placeDescription,
                    location = GeoPoint(latLng.latitude, latLng.longitude),
                    creatorID = currentUser,
                    dateCreated = dateFormatter.format(currentDate),
                    timeCreated = timeFormatter.format(currentDate),
                    imageUrl = imageUri?.toString()  // Čuvanje URL slike ako je dostupna
                )
                placeRepository.savePlace(place)
                onPlaceSaved(place) // Obaveštavanje da je mesto sačuvano
            }) {
                Text("Dodaj Mesto")
            }
        },
        dismissButton = {
            Button(onClick = {
                onDismiss()
            }) {
                Text("Otkaži")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddResponseDialog(
    place: Place?,
    requestId: String?,
    placeRepository: PlaceRepository,
    onResponseSaved: () -> Unit,
    onDismiss: () -> Unit
) {
    var comment by remember { mutableStateOf("") }
    val currentUser = FirebaseAuth.getInstance().currentUser?.email ?: "Anonymous"
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val currentDate = Date()

    AlertDialog(
        onDismissRequest = {
            onDismiss()
        },
        title = { Text("Dodaj Odgovor") },
        text = {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Mesto: ${place?.name ?: "Nepoznato"}")
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Komentar") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                place?.let {
                    val response = mapOf(
                        "placeId" to it.id,
                        "requestId" to requestId,
                        "comment" to comment,
                        "creatorID" to currentUser,
                        "dateCreated" to dateFormatter.format(currentDate),
                        "timeCreated" to timeFormatter.format(currentDate)
                    ) as Map<String, Any>
                    placeRepository.saveResponse(response)
                }
                onResponseSaved()
                onDismiss()
            }) {
                Text("Sačuvaj Odgovor")
            }
        },
        dismissButton = {
            Button(onClick = {
                onDismiss()
            }) {
                Text("Otkaži")
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
