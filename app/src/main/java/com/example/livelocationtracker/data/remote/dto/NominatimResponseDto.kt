package com.example.livelocationtracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NominatimResponseDto(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("address") val address: AddressDto? = null,
)

@Serializable
data class AddressDto(
    @SerialName("road") val road: String? = null,
    @SerialName("neighbourhood") val neighbourhood: String? = null,
    @SerialName("suburb") val suburb: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("town") val town: String? = null,
    @SerialName("village") val village: String? = null,
    @SerialName("state") val state: String? = null,
    @SerialName("country") val country: String? = null,
    @SerialName("postcode") val postcode: String? = null,
)