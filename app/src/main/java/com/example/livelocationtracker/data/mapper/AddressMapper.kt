package com.example.livelocationtracker.data.mapper

import com.example.livelocationtracker.data.remote.dto.NominatimResponseDto
import com.example.livelocationtracker.domain.model.Address

internal fun NominatimResponseDto.toDomain(): Address {
    val addr = address
    val locality = addr?.city ?: addr?.town ?: addr?.village ?: addr?.suburb ?: addr?.neighbourhood
    return Address(
        displayName = displayName.orEmpty(),
        road = addr?.road,
        city = locality,
        state = addr?.state,
        country = addr?.country,
        postcode = addr?.postcode,
    )
}