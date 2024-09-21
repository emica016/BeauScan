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
import java.util.Calendar
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
    var selectedDate by remember { mutableStateOf("") } // Dodato polje za datum

    val mapProperties = remember { MapProperties(isMyLocationEnabled = true) }
    val mapUiSettings = remember { MapUiSettings(zoomControlsEnabled = true) }

    var currentLocationMarker by remember { mutableStateOf<Marker?>(null) }
    var radiusKm by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(selectedType, radiusKm) {
        if (googleMap != null) {
            fetchPlaces(placeRepository, places, placeMarkers, googleMap, placeId, selectedType, selectedDate, placeRespond, currentLocation.value, radiusKm)
        }
    }

    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    if (locationPermissionState.status.isGranted) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Service control buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        val intent = Intent(context, LocationService::class.java).apply {
                            action = LocationService.ACTION_START
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
                                        fetchPlaces(placeRepository, places, placeMarkers, googleMap, placeId, selectedType, selectedDate,placeRespond, currentLocation.value, radiusKm)
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
                    onClick = { selectedType = null; radiusKm = null },
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ){
                Button(
                    onClick = {
                        showDatePickerDialog(context) { date ->
                            selectedDate = date
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("Izaberi Datum")
                }

                Text("Izabrani datum: $selectedDate", modifier = Modifier.padding(8.dp))
            }
            // Radius input and search/reset buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = radiusKm?.toString() ?: "",
                    onValueChange = { radiusKm = it.toDoubleOrNull() },
                    label = { Text("Unesi radijus (km)") },
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                )


                Button(
                    onClick = {
                        selectedType = null
                        radiusKm = null
                        selectedDate = null.toString()
                        coroutineScope.launch {
                            fetchPlaces(placeRepository, places, placeMarkers, googleMap, placeId, selectedType, selectedDate, placeRespond, currentLocation.value, radiusKm)
                        }
                    },
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text("Resetuj Filtere")
                }
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
                        fetchPlaces(placeRepository, places, placeMarkers, googleMap, placeId, selectedType, selectedDate,placeRespond, currentLocation.value, radiusKm)
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

private fun showDatePickerDialog(context: Context, onDateSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    android.app.DatePickerDialog(context, { _, selectedYear, selectedMonth, selectedDay ->
        val formattedDate =
            String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
        onDateSelected(formattedDate)
    }, year, month, day).show()
}
