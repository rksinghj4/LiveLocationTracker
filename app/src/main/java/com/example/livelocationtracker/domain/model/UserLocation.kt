package com.example.livelocationtracker.domain.model

/**
 * A single fix from the device's location provider.
 *
 * @property latitude  decimal degrees, WGS84.
 * @property longitude decimal degrees, WGS84.
 * @property accuracyMeters horizontal accuracy radius (1σ) reported by the OS.
 * @property timestampMillis epoch ms when the OS produced the fix (NOT when we received it).
 */
data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestampMillis: Long,
)