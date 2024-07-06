package com.example.myapplication.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.MainActivity
import com.google.android.gms.location.*
import java.text.SimpleDateFormat
import java.util.*

object LocationHelper {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    fun getFusedLocationProviderClient(context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun getLastLocation(activity: MainActivity, locationText: TextView, eventTimeText: TextView) {
        fusedLocationClient = getFusedLocationProviderClient(activity)

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkLocationPermission(activity)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    updateLocationUI(location, locationText, eventTimeText)
                } else {
                    requestLocationUpdates(activity, locationText, eventTimeText)
                }
            }
            .addOnFailureListener {
                requestLocationUpdates(activity, locationText, eventTimeText)
            }
    }

    private fun requestLocationUpdates(activity: MainActivity, locationText: TextView, eventTimeText: TextView) {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    updateLocationUI(location, locationText, eventTimeText)
                }
                fusedLocationClient.removeLocationUpdates(this)
            }
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkLocationPermission(activity)
            return
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun updateLocationUI(location: Location, locationText: TextView, eventTimeText: TextView) {
        val latitude = location.latitude
        val longitude = location.longitude
        locationText.text = "Location: Lat: $latitude, Long: $longitude"

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val eventTime = dateFormat.format(Date(System.currentTimeMillis()))
        eventTimeText.text = "Event Time: $eventTime"

        MainActivity.longitude_main = longitude
        MainActivity.latitude_main = latitude
    }

    fun checkLocationPermission(activity: MainActivity) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                MainActivity.LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }
}
