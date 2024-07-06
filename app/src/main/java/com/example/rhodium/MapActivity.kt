package com.example.myapplication

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.database.NetworkInfoDatabaseHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.example.myapplication.databinding.ActivityMapsBinding

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var dbHelper: NetworkInfoDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        dbHelper = NetworkInfoDatabaseHelper(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val data = dbHelper.getAllInfo()
        Log.d("MapActivity", "Data retrieved: ${data.size} entries")

        if (data.isEmpty()) {
            Log.d("MapActivity", "No data found in the database.")
            return
        }

        for (entry in data) {
            val location = LatLng(entry.latitude, entry.longitude)
            val color = getColorForSituation(entry.situation)

            val markerOptions = MarkerOptions()
                .position(location)
                .title(entry.situation)
                .snippet("""
                    Location: (${entry.latitude}, ${entry.longitude})
                    PLMN ID: ${entry.plmnId}
                    LAC: ${entry.lac}
                    RAC: ${entry.rac}
                    TAC: ${entry.tac}
                    Cell ID: ${entry.cellId}
                    Signal Strength: ${entry.signalStrength} dBm
                    RSRQ: ${entry.rsrq}
                    RSRP: ${entry.rsrp}
                    RSCP: ${entry.rscp}
                    EC/No: ${entry.ecNo}
                    Technology: ${entry.cellTechnology}
                """.trimIndent())
                .icon(BitmapDescriptorFactory.defaultMarker(color))

            mMap.addMarker(markerOptions)

            val currentIndex = data.indexOf(entry)
            if (currentIndex > 0) {
                val previousLocation = LatLng(data[currentIndex - 1].latitude, data[currentIndex - 1].longitude)
                mMap.addPolyline(PolylineOptions().add(previousLocation, location).color(color.toInt()))
            }
        }

        if (data.isNotEmpty()) {
            val firstLocation = LatLng(data[0].latitude, data[0].longitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 10f))
        }

        mMap.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View? {
                return null
            }

            override fun getInfoContents(marker: Marker): View? {
                val infoView = layoutInflater.inflate(R.layout.custom_info_window, null)
                val title = infoView.findViewById<TextView>(R.id.title)
                val snippet = infoView.findViewById<TextView>(R.id.snippet)

                title.text = marker.title
                snippet.text = marker.snippet

                return infoView
            }
        })

        mMap.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            true
        }
    }

    private fun getColorForSituation(situation: String): Float {
        return when (situation.toLowerCase()) {
            "very poor" -> BitmapDescriptorFactory.HUE_BLACK
            "poor" -> BitmapDescriptorFactory.HUE_RED
            "fair" -> BitmapDescriptorFactory.HUE_ORANGE
            "good" -> BitmapDescriptorFactory.HUE_YELLOW
            "excellent" -> BitmapDescriptorFactory.HUE_GREEN
            else -> BitmapDescriptorFactory.HUE_VIOLET
        }
    }
}
