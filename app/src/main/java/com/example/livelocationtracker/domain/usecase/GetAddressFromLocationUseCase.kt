package com.example.livelocationtracker.domain.usecase

import com.example.livelocationtracker.domain.model.Address
import com.example.livelocationtracker.domain.repository.GeocodingRepository
import javax.inject.Inject

class GetAddressFromLocationUseCase @Inject constructor(
    private val repository: GeocodingRepository,
) {
    suspend operator fun invoke(latitude: Double, longitude: Double): Result<Address> =
        repository.reverseGeocode(latitude, longitude)
}