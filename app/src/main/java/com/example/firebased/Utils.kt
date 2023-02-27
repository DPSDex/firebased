package com.example.firebased

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.*
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat

import java.util.*

fun getLocation(context: Context): String {
    var currentLocation: Location? = null
    lateinit var locationManager: LocationManager
    locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
//------------------------------------------------------//
    val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    var locationByGps: Location? = null
    var locationByNetwork: Location? = null
    var latitude : Double? = null
    var longitude : Double? = null
    val gpsLocationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            locationByGps= location
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }
//------------------------------------------------------//
    val networkLocationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            locationByNetwork= location
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }



    if (hasGps) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return "not found"
        }
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            5000,
            0F,
            gpsLocationListener
        )
    }
//------------------------------------------------------//
    if (hasNetwork) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return "not found"
        }
        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            5000,
            0F,
            networkLocationListener
        )
    }

    val lastKnownLocationByGps =
        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    lastKnownLocationByGps?.let {
        locationByGps = lastKnownLocationByGps
        latitude = locationByGps?.latitude
        longitude = locationByGps?.longitude
    }
//------------------------------------------------------//
    val lastKnownLocationByNetwork =
        locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    lastKnownLocationByNetwork?.let {
        locationByNetwork = lastKnownLocationByNetwork
        latitude = locationByNetwork!!.latitude
        longitude = locationByNetwork!!.longitude
    }
//------------------------------------------------------//
    if (locationByGps != null && locationByNetwork != null) {
        if (locationByGps!!.accuracy > locationByNetwork!!.accuracy) {
            currentLocation = locationByGps
            if (currentLocation != null) {
                latitude = currentLocation.latitude
            }
            if (currentLocation != null) {
                longitude = currentLocation.longitude
            }
            // use latitude and longitude as per your need
        } else {
            currentLocation = locationByNetwork
            if (currentLocation != null) {
                latitude = currentLocation.latitude
            }
            if (currentLocation != null) {
                longitude = currentLocation.longitude
            }
            // use latitude and longitude as per your need
        }
    }

    val geocoder = Geocoder(context, Locale.getDefault())
    var currentloc = "not found"
    if(latitude != null && longitude!=null) {
        val addresses: List<Address>? = geocoder.getFromLocation(latitude!!, longitude!!, 1)
        val cityName: String = addresses!![0].getAddressLine(0)
        //val stateName: String = addresses!![0].getAddressLine(1)
        //val countryName: String = addresses!![0].getAddressLine(2)
        Log.d("Utils", "cityName: " + cityName)
        currentloc = cityName
    }

//    //Variables
//    //val local = Locale("en_us", "United States")
//    val geocoder = Geocoder(context, Locale.getDefault())
////    val latitude = 18.185600
////    val longitude = 76.041702
//    val maxResult = 1
//
//
////Fetch address from location
//    if (latitude != null && longitude != null) {
//        geocoder.getFromLocation(latitude,longitude,maxResult,object : Geocoder.GeocodeListener{
//            override fun onGeocode(addresses: MutableList<Address>) {
//                val cityName: String = addresses!![0].getAddressLine(0)
//                currentloc = cityName
//                Log.d("DashboardActivity", "found" + cityName)
//                // code
//            }
//
//            override fun onError(errorMessage: String?) {
//                super.onError(errorMessage)
//
//            }
//
//        })
//    }

    return currentloc
}