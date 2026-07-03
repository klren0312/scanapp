package com.example.scanapp.service

import com.example.scanapp.models.LocationRecord
import kotlinx.coroutines.flow.StateFlow

interface LocationService {
    val currentLocation: StateFlow<LocationRecord?>
    val isTracking: StateFlow<Boolean>

    suspend fun startTracking()
    suspend fun stopTracking()
    suspend fun getCurrentLocation(): LocationRecord?
}
