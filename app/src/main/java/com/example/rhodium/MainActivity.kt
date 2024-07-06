package com.example.myapplication
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import com.example.myapplication.database.NetworkInfoDatabaseHelper
import com.example.myapplication.location.LocationHelper
import com.example.myapplication.network.NetworkHelper

class MainActivity : AppCompatActivity() {

    private lateinit var locationText: TextView
    private lateinit var eventTimeText: TextView
    private lateinit var cellTechText: TextView
    private lateinit var cellLocationText: TextView
    private lateinit var signalQualityText: TextView
    private lateinit var stopInsertionButton: Button

    private lateinit var db: NetworkInfoDatabaseHelper

    private val handler = Handler(Looper.getMainLooper())
    private var isInsertionStopped = false

    private val PERMISSIONS_REQUEST_CODE = 123

    private val runnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.P)
        override fun run() {
            if (!isInsertionStopped) {
                LocationHelper.getLastLocation(this@MainActivity, locationText, eventTimeText)
                NetworkHelper.displayNetworkInfo(this@MainActivity, cellTechText, cellLocationText, signalQualityText)
            }
            handler.postDelayed(this, 1000)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationText = findViewById(R.id.locationText)
        eventTimeText = findViewById(R.id.eventTimeText)
        cellTechText = findViewById(R.id.cellTechText)
        cellLocationText = findViewById(R.id.cellLocationText)
        signalQualityText = findViewById(R.id.signalQualityText)
        stopInsertionButton = findViewById(R.id.btn_stop_insertion)
        val buttonOpenMap = findViewById<Button>(R.id.btn_open_map)

        db = NetworkInfoDatabaseHelper(this)
        val networkInfoList = db.getAllInfo()
        val listView: ListView = findViewById(R.id.listView)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, networkInfoList.map { it.toString() })
        listView.adapter = adapter

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE
                ),
                PERMISSIONS_REQUEST_CODE
            )
        } else {
            startServicesAndTasks()
        }

        buttonOpenMap.setOnClickListener {
            startActivity(Intent(this, MapsActivity::class.java))
        }

        stopInsertionButton.setOnClickListener {
            isInsertionStopped = !isInsertionStopped
            stopInsertionButton.text = if (isInsertionStopped) "Resume Insertion" else "Stop Insertion"
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startServicesAndTasks()
            } else {
                locationText.text = "Location permission denied"
                eventTimeText.text = ""
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun startServicesAndTasks() {
        handler.post(runnable)
        val cellularService = CellularService(this)
        cellularService.startCollectingData()
        Log.d("MainActivity", "Services started")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }
}
