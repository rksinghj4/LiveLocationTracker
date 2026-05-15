package com.example.livelocationtracker.domain.usecase

import com.example.livelocationtracker.domain.model.UserLocation
import com.example.livelocationtracker.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLiveLocationUseCase @Inject constructor(
    private val repository: LocationRepository,
) {
    operator fun invoke(intervalMillis: Long = 5_000L): Flow<UserLocation> =
        repository.observeLocationUpdates(intervalMillis)

    fun isLocationEnabled(): Boolean = repository.isLocationEnabled()
}