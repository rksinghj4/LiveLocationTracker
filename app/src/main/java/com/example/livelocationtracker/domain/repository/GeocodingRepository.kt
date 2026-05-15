package com.example.livelocationtracker.domain.repository

import com.example.livelocationtracker.domain.model.Address

interface GeocodingRepository {
    suspend fun reverseGeocode(latitude: Double, longitude: Double): Result<Address>
}