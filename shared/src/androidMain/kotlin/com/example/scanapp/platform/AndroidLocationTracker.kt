package com.example.scanapp.platform

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.example.scanapp.models.LocationRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AndroidLocationTracker(private val context: Context) {
    
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val _currentLocation = MutableStateFlow<LocationRecord?>(null)
    val currentLocation: StateFlow<LocationRecord?> = _currentLocation
    
    private var locationListener: LocationListener? = null
    
    @SuppressLint("MissingPermission")
    fun startTracking() {
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val record = LocationRecord(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    accuracy = location.accuracy,
                    timestamp = location.time
                )
                _currentLocation.value = record
            }
            
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        
        // 尝试使用GPS提供商
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L, // 1秒更新间隔
                1f, // 1米最小距离
                locationListener!!
            )
        }
        
        // 尝试使用网络提供商
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1000L,
                1f,
                locationListener!!
            )
        }
    }
    
    fun stopTracking() {
        locationListener?.let { locationManager.removeUpdates(it) }
        locationListener = null
    }
    
    fun getCurrentLocation(): LocationRecord? {
        return _currentLocation.value
    }
}