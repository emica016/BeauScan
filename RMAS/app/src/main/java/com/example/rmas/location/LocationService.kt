package com.example.rmas.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.rmas.MainActivity
import com.example.rmas.R
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LocationService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient
    private lateinit var sharedPreferences: SharedPreferences
    private val notifiedPlaces = mutableSetOf<String>()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        createNotificationChannel()
        locationClient = LocationClientImpl(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("LocationService", "Service started with action: ${intent?.action}")

        when (intent?.action) {
            ACTION_START -> {
                Log.d("LocationService", "Service started")
                val notification = createNotification()
                startForeground(NOTIFICATION_ID, notification)
                start(placeIsNearby = true)  // Pokreće pronalaženje bliskih mesta
            }
            ACTION_STOP -> {
                Log.d("LocationService", "Service stopped")
                stop()
            }
            ACTION_FIND_NEARBY -> {
                Log.d("LocationService", "Service started for nearby places")
                val notification = createNotification()
                startForeground(NOTIFICATION_ID, notification)
                start(placeIsNearby = true)  // Takođe pokreće pronalaženje bliskih mesta
            }
        }
        return START_NOT_STICKY
    }

    fun start(placeIsNearby: Boolean = false) {
        locationClient.getLocationUpdates(3000L)
            .catch { e -> e.printStackTrace() }
            .onEach { location ->
                sharedPreferences.edit()
                    .putString("last_latitude", location.latitude.toString())
                    .putString("last_longitude", location.longitude.toString())
                    .apply()

                val intent = Intent(ACTION_LOCATION_UPDATE).apply {
                    putExtra(EXTRA_LOCATION_LATITUDE, location.latitude)
                    putExtra(EXTRA_LOCATION_LONGITUDE, location.longitude)
                }
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

                // Ako je postavljen flag za bliska mesta, proveri bliskost
                if (placeIsNearby) {
                    checkProximityToPlaces(location.latitude, location.longitude)
                }
            }.launchIn(serviceScope)
    }

    private fun stop() {
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("LocationService", "Service stopped")
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        val notificationChannelId = "LOCATION_SERVICE_CHANNEL"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "Lokacija",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Obaveštavamo vas da se vaša lokacija prati u pozadini"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): android.app.Notification {
        val notificationChannelId = "LOCATION_SERVICE_CHANNEL"
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Praćenje lokacije")
            .setContentText("Servis praćenja lokacije je pokrenut u pozadini")
            .setSmallIcon(R.drawable.place_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun calculateHaversineDistance(
        lat1: Double, lon1: Double, lat2: Double, lon2: Double
    ): Double {
        val R = 6371e3 // Earth radius in meters
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLatRad = Math.toRadians(lat2 - lat1)
        val deltaLonRad = Math.toRadians(lon2 - lon1)

        val a = sin(deltaLatRad / 2) * sin(deltaLatRad / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLonRad / 2) * sin(deltaLonRad / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c
    }

    private fun checkProximityToPlaces(latitude: Double, longitude: Double) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("places")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val geoPoint = document.getGeoPoint("location") ?: continue
                    val placeLatitude = geoPoint.latitude
                    val placeLongitude = geoPoint.longitude
                    val distance = calculateHaversineDistance(latitude, longitude, placeLatitude, placeLongitude)

                    if (distance < PROXIMITY_RADIUS && !notifiedPlaces.contains(document.id)) {
                        Log.d("LocationService", "Nearby place found: ${document.id}")
                        sendProximityNotification(document.id, distance) // Notify user
                        notifiedPlaces.add(document.id)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("LocationService", "Error checking proximity", e)
            }
    }

    private fun sendProximityNotification(placeId: String, distance: Double) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_PLACE_ID, placeId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, "LOCATION_SERVICE_CHANNEL")
            .setContentTitle("Mesto u blizini!")
            .setContentText("Pronađeno mesto $placeId na udaljenosti od $distance metara.")
            .setSmallIcon(R.drawable.place_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(placeId.hashCode(), notification) // Unique ID for each place
    }

    companion object {
        const val ACTION_START = "com.example.rmas.action.START"
        const val ACTION_STOP = "com.example.rmas.action.STOP"
        const val ACTION_FIND_NEARBY = "com.example.rmas.action.FIND_NEARBY"
        const val ACTION_LOCATION_UPDATE = "com.example.rmas.action.LOCATION_UPDATE"
        const val EXTRA_LOCATION_LATITUDE = "com.example.rmas.extra.LOCATION_LATITUDE"
        const val EXTRA_LOCATION_LONGITUDE = "com.example.rmas.extra.LOCATION_LONGITUDE"
        const val NOTIFICATION_ID = 1234
        const val PROXIMITY_RADIUS = 1000 // Proximity radius in meters
        const val EXTRA_PLACE_ID = "com.example.rmas.extra.PLACE_ID"
    }
}
