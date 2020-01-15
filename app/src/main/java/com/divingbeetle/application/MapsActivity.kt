package com.divingbeetle.application

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import android.database.Cursor
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
    OnRequestPermissionsResultCallback {
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    private lateinit var mDbHelper: DBHelper
    private lateinit var mMap: GoogleMap
    private lateinit var mMapFragment: SupportMapFragment

    private lateinit var mMarkerOptionsList: ArrayList<MarkerOptions>
    private lateinit var mToiletList: ArrayList<HashMap<String, Any>>
    private lateinit var mListView: ListView

    private var mToggleListView = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mMapFragment.getMapAsync(this)

        fusedLocationClient = FusedLocationProviderClient(this.applicationContext)
        mDbHelper = DBHelper(this.applicationContext)
        mListView = findViewById(R.id.list_view)

        //=============================
        // Refresh Button
        //=============================
        val btnRefresh: Button = findViewById(R.id.btnRefresh)
        val btnRefreshList: Button = findViewById(R.id.btnRefreshList)
        val onClickListener = View.OnClickListener { setUpMap() }
        btnRefresh.setOnClickListener(onClickListener)
        btnRefreshList.setOnClickListener(onClickListener)

        //=============================
        // ListView Toggle
        //=============================
        val llMapArea: LinearLayout = findViewById(R.id.llMapArea)

        val llListTitle: LinearLayout = findViewById(R.id.llListTitle)
        llListTitle.setOnClickListener{
            val lpMapArea:RelativeLayout.LayoutParams = llMapArea.layoutParams as RelativeLayout.LayoutParams

            if (mToggleListView) {
                mListView.visibility = View.VISIBLE

                lpMapArea.topMargin = 40
                lpMapArea.bottomMargin = 240
            }
            else {
                mListView.visibility = View.GONE

                lpMapArea.topMargin = 40
                lpMapArea.bottomMargin = 0
            }

            mToggleListView = !mToggleListView
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setPadding(0, 10, 0, 10)
        mMap.setOnMarkerClickListener(this)

        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            setUpMap()
        }
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private fun setUpMap() {
        mMap.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            // Got last known location. In some rare situations this can be null.
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16.0f))

                //=============================
                // Load Toilet Data
                //=============================
                val areaSizeInterval = 0.025f
                val maxAreaSize = 0.2f
                val minimumNumber = 50

                var areaSize = 0.025f
                var cursor = loadToiletData(location, areaSize)

                while (cursor.count < minimumNumber && areaSize < maxAreaSize) {
                    areaSize += areaSizeInterval
                    cursor = loadToiletData(location, areaSize)
                }

                if (cursor.count <= 0) {
                    Toast.makeText(this.applicationContext, R.string.location_warning, Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                mToiletList = ArrayList()
                cursor.moveToFirst()

                while (cursor.moveToNext()) {
                    val toiletLatitude = cursor.getDouble(cursor.getColumnIndex("위도"))
                    val toiletLongitude = cursor.getDouble(cursor.getColumnIndex("경도"))

                    val toiletLocation = Location("")
                    toiletLocation.latitude = toiletLatitude
                    toiletLocation.longitude = toiletLongitude

                    val distance: Float = toiletLocation.distanceTo(location)

                    val toiletDataMap = HashMap<String, Any>()
                    toiletDataMap["화장실명"] = cursor.getString(cursor.getColumnIndex("화장실명"))
                    toiletDataMap["구분"] = cursor.getString(cursor.getColumnIndex("구분"))
                    toiletDataMap["남녀공용화장실여부"] = cursor.getString(cursor.getColumnIndex("남녀공용화장실여부"))
                    toiletDataMap["위도"] = toiletLatitude
                    toiletDataMap["경도"] = toiletLongitude
                    toiletDataMap["거리"] = distance

                    val distanceText: String
                    distanceText = if (distance >= 1000) {
                        String.format("%.2f", distance / 1000) + "km"
                    } else {
                        String.format("%d", distance.toInt()) + "m"
                    }

                    toiletDataMap["거리Text"] = distanceText

                    mToiletList.add(toiletDataMap)
                }
                cursor.close()

                //=============================
                // Mark Toilets on the Map
                //=============================
                mToiletList.sortWith(compareBy { it["거리"] as Comparable<*>? })
                mMarkerOptionsList = ArrayList()

                //for (index in 0.<mToiletList.count()) {
                mToiletList.forEachIndexed { index, toiletDataMap ->
                    toiletDataMap["index"] = index

                    val markerOptions = MarkerOptions()
                    val latitude: Double = toiletDataMap["위도"].toString().toDouble()
                    val longitude: Double = toiletDataMap["경도"].toString().toDouble()
                    val distanceText = toiletDataMap["거리Text"].toString()

                    markerOptions.position(LatLng(latitude, longitude))
                    markerOptions.title(toiletDataMap["화장실명"].toString())

                    val description = toiletDataMap["구분"].toString() + " " + distanceText
                    markerOptions.snippet(description)

                    val marker = mMap.addMarker(markerOptions)
                    marker.tag = index
                }

                //=============================
                // Init List
                //=============================
                mListView.adapter = ListViewAdapter(this, mToiletList)
                mListView.onItemClickListener =
                    AdapterView.OnItemClickListener { _, _, position, _ ->
                        mListView.setSelection(position)

                        val toiletDataMap = mToiletList[position]
                        val dLatitude: Double = toiletDataMap["위도"].toString().toDouble()
                        val dLongitude: Double = toiletDataMap["경도"].toString().toDouble()

                        val listLatLng = LatLng(dLatitude, dLongitude)
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(listLatLng, 17.0f))
                    }

            }
        }
    }

    private fun loadToiletData(location: Location, areaSize: Float): Cursor {
        val minLatitude = location.latitude - areaSize / 2
        val maxLatitude = location.latitude + areaSize / 2

        val minLongitude = location.longitude - areaSize / 2
        val maxLongitude = location.longitude + areaSize / 2

        val query = "SELECT * " +
                "FROM Toilets " +
                "WHERE 위도 >= $minLatitude " +
                "AND 위도 <= $maxLatitude " +
                "AND 경도 >= $minLongitude " +
                "AND 경도 <= $maxLongitude " +
                "Limit 500"

        val database = mDbHelper.database

        return database.rawQuery(query, null)
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        if (marker != null) {
            val index = marker.tag.toString().toInt()
            if (index > 0) {
                mListView.smoothScrollToPosition(index)
                mListView.setSelection(index)
            }
            return false
        }

        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setUpMap()
            } else {
                Toast.makeText(this.applicationContext, R.string.permission_warning, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onStop() {
        mDbHelper.close()
        super.onStop()
    }
}
