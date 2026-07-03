package com.example.scanapp.service

import android.content.Context
import com.example.scanapp.models.LocationRecord
import com.example.scanapp.platform.AndroidLocationTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AndroidLocationService(context: Context) : LocationService {

    private val locationTracker = AndroidLocationTracker(context)

    override val currentLocation: StateFlow<LocationRecord?> = locationTracker.currentLocation

    private val _isTracking = MutableStateFlow(false)
    override val isTracking: StateFlow<Boolean> = _isTracking

    override suspend fun startTracking() {
        locationTracker.startTracking()
        _isTracking.value = true
    }

    override suspend fun stopTracking() {
        locationTracker.stopTracking()
        _isTracking.value = false
    }

    override suspend fun getCurrentLocation(): LocationRecord? {
        return locationTracker.getCurrentLocation()
    }
}
