package com.example.rmas.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class LocationClientImpl(
    private val context: Context, //za pristupanje resursima i servisima sistema
    private val client: FusedLocationProviderClient //pbezbedjuje objedinjene usluge lokacije
) : LocationClient {
    override fun getLocationUpdates(interval: Long): Flow<Location> { //Flow je Kotlinova korutinska struktura koja omogucava emitovanje vise vrednosti tokom vremena.
        return callbackFlow {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                throw LocationClient.LocationException("Nedostaje odobrenje za lokaciju")
            }
//            if(!context.hasLocationPermission()){
//                throw LocationClient.LocationException("Nedostaje odobrenje za lokaciju")
//            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if(!isGpsEnabled && !isNetworkEnabled){
                throw LocationClient.LocationException("GPS je onemogućen")
            }

            val request = LocationRequest.create()
                .setInterval(interval) //interval u kojem aplikacija zeli da dobija azuriranje lokacije
                .setFastestInterval(interval) //najkraci moguci interval izmedju azuriranja

            val locationCallback = object : LocationCallback(){ //reaguje na rezultate lokacije
                override fun onLocationResult(result: LocationResult) { //kad god se dobije nova lokacija ova metoda se poziva
                    super.onLocationResult(result)
                    result.locations.lastOrNull()?.let { location ->
                        launch { send(location) } //poslednja dobijena lokacija se salje kroz Flow
                    }
                }
            }

            client.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper() //kako bi azuriranja stigla na glavnu nit
            )

            awaitClose { //da se osigura da se lokacija vise nece azurirati kada nije potrebna
                client.removeLocationUpdates(locationCallback)
            }
        }
    }
}