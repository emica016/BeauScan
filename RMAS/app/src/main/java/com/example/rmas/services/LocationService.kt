package com.example.rmas.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import com.example.rmas.R
import com.example.rmas.data.DefaultLocationClient
import com.example.rmas.data.LocationInfo
import com.example.rmas.data.LocationClient
import com.example.rmas.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LocationService: Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()

        val locationChannel = NotificationChannel(
            "locationservicechannel",
            "Location",
            NotificationManager.IMPORTANCE_LOW
        )
        val eventChannel = NotificationChannel(
            "eventservicechannel",
            "Event",
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(locationChannel)
        notificationManager.createNotificationChannel(eventChannel)

        locationClient = DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        start()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start() {
        var eventNotificationSent = false
        // must fix this, wont work for other events

        val locationNotification = NotificationCompat.Builder(this, "locationservicechannel")
            .setContentTitle("Tracking location...")
            .setContentText("Location: unknown")
            .setSmallIcon(R.drawable.logo)
            .setOngoing(true)

        val eventNotification = NotificationCompat.Builder(this, "eventservicechannel")
            .setContentTitle("Welcome to the event!")
            .setSmallIcon(R.drawable.logo)
            .setOngoing(false)
            .setAutoCancel(true)


        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        locationClient
            .getLocationUpdates(10000L)
            .onEach { location ->
                Log.d("SERVICE", location.toString())
                LocationInfo.location = location
                LocationInfo.alert()
                val lat = location.latitude.toString()
                val long = location.longitude.toString()
                FirebaseDatabase.getPlacesAtCurrentLocation(location.latitude, location.longitude) { events ->
                    for(event in events) {
                        Log.e("LOCATION SERVICE", "start: ${event.title}", )
                        FirebaseDatabase.checkUserAttendance(event.name) { beenThere ->
                            if(beenThere) {
                                Log.e("LOCATION SERVICE", "start: already been there.", )
                            } else {
                                if (!eventNotificationSent) {
                                    val like = Intent(
                                        applicationContext,
                                        NotificationActionReceiver::class.java
                                    ).apply {
                                        action = "like|${event.name}|${event.creatorID}"
                                    }
                                    val pendingLike = PendingIntent.getBroadcast(
                                        applicationContext,
                                        0,
                                        like,
                                        PendingIntent.FLAG_IMMUTABLE
                                    )

                                    val updatedNotification = eventNotification.setStyle(
                                        NotificationCompat.BigTextStyle()
                                            .bigText("Hey, thanks for attending ${event.title}!\nIf you like the event, tap button below to give point to the creator.")
                                    ).addAction(
                                        // add the action button to notification
                                        NotificationCompat.Action(
                                            R.drawable.logo,
                                            "I like the event",
                                            pendingLike,
                                        )
                                    )
                                    notificationManager.notify(2, updatedNotification.build())
                                    eventNotificationSent = true
                                }
                            }

                        }
                    }
                }
                val updatedNotification = locationNotification.setContentText(
                    "Location: ($lat, $long)"
                )
                notificationManager.notify(1, updatedNotification.build())
            }
            .launchIn(serviceScope)

        Log.d("LOCATION SERVICE", "Service started.")
        startForeground(1, locationNotification.build())
    }

    private fun stop() {
        Log.d("LOCATION SERVICE", "Service stopped.")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun stopService(name: Intent?): Boolean {
        stop()
        return super.stopService(name)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val arguments = intent?.action?.split("|")
        if(arguments!!.isEmpty()) return

        if (arguments[0] == "like") {
            // Call your function here
            val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(2)
            Log.e("LOCATION SERVICE", "onReceive: like")
            FirebaseDatabase.setUserAttendance(arguments[1])
            FirebaseDatabase.incrementUserScore(arguments[2])
        }
    }
}