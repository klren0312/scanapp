package com.example.scanapp.models

import kotlinx.serialization.Serializable

@Serializable
data class LocationRecord(
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
    val timestamp: Long
)