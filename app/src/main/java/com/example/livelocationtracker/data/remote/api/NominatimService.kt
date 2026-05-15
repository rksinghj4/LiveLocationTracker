package com.example.livelocationtracker.data.remote.api

import com.example.livelocationtracker.data.remote.dto.NominatimResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Nominatim public reverse-geocoding API.
 *
 * Per the OSM(OpenStreetMap) Nominatim usage policy
 * (https://operations.osmfoundation.org/policies/nominatim/), every request must:
 *   - send an identifying User-Agent (set in [com.example.livelocationtracker.di.NetworkModule]),
 *   - rate-limit to ≤1 req/sec (the ViewModel reverse-geocodes on coarse lat/lng changes only).
 */
interface NominatimService {
    /**
     * Look up addresses for a location (Reverse geocoding)
     * Given a latitude and longitude anywhere on the planet, Nominatim can find the nearest address.
     * It can do the same for any OSM object given its ID.
     */
    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("format") format: String = "jsonv2",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("zoom") zoom: Int = 18,
    ): NominatimResponseDto
}