package com.example.myapplication.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.example.myapplication.MainActivity

data class NetworkInfo(
    val eventTime: Long,
    val latitude: Double,
    val longitude: Double,
    val cellTechnology: String?,
    val plmnId: String?,
    val rac: String?,
    val tac: String?,
    val lac: String?,
    val cellId: String?,
    val signalStrength: Int?,
    val rsrq: Int?,
    val rsrp: Int?,
    val rscp: Int?,
    val ecNo: Int?,
    val signalQuality: String,
    val situation: String
)

class NetworkInfoDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "cellular_data.db"
        private const val DATABASE_VERSION = 2
        private const val TABLE_NAME = "cellular_info"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                eventTime INTEGER,
                latitude REAL,
                longitude REAL,
                cellTechnology TEXT,
                plmnId TEXT,
                rac TEXT,
                tac TEXT,
                lac TEXT,
                cellId TEXT,
                signalStrength INTEGER,
                rsrq INTEGER,
                rsrp INTEGER,
                rscp INTEGER,
                ecNo INTEGER,
                signalQuality TEXT,
                situation TEXT
            )
        """.trimIndent()
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertInfo(networkInfo: NetworkInfo) {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put("eventTime", networkInfo.eventTime)
            put("latitude", networkInfo.latitude)
            put("longitude", networkInfo.longitude)
            put("cellTechnology", networkInfo.cellTechnology)
            put("plmnId", networkInfo.plmnId)
            put("rac", networkInfo.rac)
            put("tac", networkInfo.tac)
            put("lac", networkInfo.lac)
            put("cellId", networkInfo.cellId)
            put("signalStrength", networkInfo.signalStrength)
            put("rsrq", networkInfo.rsrq)
            put("rsrp", networkInfo.rsrp)
            put("rscp", networkInfo.rscp)
            put("ecNo", networkInfo.ecNo)
            put("signalQuality", networkInfo.signalQuality)
            put("situation", networkInfo.situation)
        }

        db.insert(TABLE_NAME, null, contentValues)
        db.close()
    }

    fun getAllInfo(): List<NetworkInfo> {
        val dataList = mutableListOf<NetworkInfo>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)

        if (cursor.moveToFirst()) {
            do {
                val networkInfo = NetworkInfo(
                    eventTime = cursor.getLong(cursor.getColumnIndexOrThrow("eventTime")),
                    latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                    longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                    cellTechnology = cursor.getString(cursor.getColumnIndexOrThrow("cellTechnology")),
                    plmnId = cursor.getString(cursor.getColumnIndexOrThrow("plmnId")),
                    rac = cursor.getString(cursor.getColumnIndexOrThrow("rac")),
                    tac = cursor.getString(cursor.getColumnIndexOrThrow("tac")),
                    lac = cursor.getString(cursor.getColumnIndexOrThrow("lac")),
                    cellId = cursor.getString(cursor.getColumnIndexOrThrow("cellId")),
                    signalStrength = cursor.getInt(cursor.getColumnIndexOrThrow("signalStrength")),
                    rsrq = cursor.getInt(cursor.getColumnIndexOrThrow("rsrq")),
                    rsrp = cursor.getInt(cursor.getColumnIndexOrThrow("rsrp")),
                    rscp = cursor.getInt(cursor.getColumnIndexOrThrow("rscp")),
                    ecNo = cursor.getInt(cursor.getColumnIndexOrThrow("ecNo")),
                    signalQuality = cursor.getString(cursor.getColumnIndexOrThrow("signalQuality")),
                    situation = cursor.getString(cursor.getColumnIndexOrThrow("situation"))
                )
                dataList.add(networkInfo)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return dataList
    }
}

object DatabaseHelper {

    @RequiresApi(Build.VERSION_CODES.P)
    fun insertNetworkInfoToDatabase(activity: MainActivity, signalStrength: Int?, rsrq: Int?, rsrp: Int?, rscp: Int?, ecNo: Int?, plmnId: String?, rac: String?, tac: String?, lac: String?, cellId: String?) {
        val eventTime = System.currentTimeMillis()
        val location = Location(LocationManager.GPS_PROVIDER)
        var latitude = location.latitude
        var longitude = location.longitude
        val cellTechnology = getCellTechnology(activity)

        val situation = calculateSituation(signalStrength)
        val color = getSituationColor(situation)

        val networkInfo = NetworkInfo(
            eventTime = eventTime,
            latitude = MainActivity.latitude_main,
            longitude = MainActivity.longitude_main,
            cellTechnology = cellTechnology,
            plmnId = plmnId,
            rac = rac,
            tac = tac,
            lac = lac,
            cellId = cellId,
            signalStrength = signalStrength,
            rsrq = rsrq,
            rsrp = rsrp,
            rscp = rscp,
            ecNo = ecNo,
            signalQuality = "Signal Strength: $signalStrength dBm, RSRQ: $rsrq, RSRP: $rsrp, RSCP: $rscp, EC/No: $ecNo",
            situation = "$situation ($color)"
        )

        val db = NetworkInfoDatabaseHelper(activity)
        db.insertInfo(networkInfo)
    }

    private fun calculateSituation(signalStrength: Int?): String {
        return when (signalStrength) {
            null -> "Unknown"
            10000 -> "Unknown"
            in -85..Int.MAX_VALUE -> "Excellent"
            in -95..-86 -> "Good"
            in -105..-96 -> "Fair"
            in -115..-106 -> "Poor"
            else -> "Very Poor"
        }
    }

    private fun getSituationColor(situation: String): String {
        return when (situation) {
            "Excellent" -> "green"
            "Good" -> "yellow"
            "Fair" -> "orange"
            "Poor" -> "red"
            "Very Poor" -> "black"
            else -> "unknown"
        }
    }

    private fun getCellTechnology(activity: MainActivity): String {
        val telephonyManager = activity.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return try {
            when (telephonyManager.networkType) {
                TelephonyManager.NETWORK_TYPE_LTE -> "4G (LTE)"
                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                TelephonyManager.NETWORK_TYPE_HSPAP -> "3G (HSPA+)"
                TelephonyManager.NETWORK_TYPE_HSPA -> "3G (HSPA)"
                TelephonyManager.NETWORK_TYPE_UMTS -> "3G (UMTS)"
                TelephonyManager.NETWORK_TYPE_EDGE -> "2G (EDGE)"
                TelephonyManager.NETWORK_TYPE_GPRS -> "2G (GPRS)"
                else -> "Unknown"
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            "Permission Denied"
        }
    }
}
