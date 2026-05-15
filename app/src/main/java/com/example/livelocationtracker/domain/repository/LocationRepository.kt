package com.example.livelocationtracker.domain.repository

import com.example.livelocationtracker.domain.model.UserLocation
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    /**
     * Cold flow of fixes. Collection starts the underlying provider; cancellation stops it.
     * Throws [com.example.livelocationtracker.domain.model.LocationError] subclasses when it
     * cannot deliver (no permission, services off, etc.).
     */
    fun observeLocationUpdates(intervalMillis: Long = 5_000L): Flow<UserLocation>

    /** True if device location services (GPS or network) are currently enabled. */
    fun isLocationEnabled(): Boolean
}