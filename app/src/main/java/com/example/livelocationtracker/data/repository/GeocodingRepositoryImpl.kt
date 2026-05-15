package com.example.livelocationtracker.data.repository

import com.example.livelocationtracker.data.mapper.toDomain
import com.example.livelocationtracker.data.remote.api.NominatimService
import com.example.livelocationtracker.domain.model.Address
import com.example.livelocationtracker.domain.model.LocationError
import com.example.livelocationtracker.domain.repository.GeocodingRepository
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeocodingRepositoryImpl @Inject constructor(
    private val service: NominatimService,
) : GeocodingRepository {

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): Result<Address> {
        return try {
            val dto = service.reverseGeocode(latitude, longitude)
            Result.success(dto.toDomain())
        } catch (ce: CancellationException) {
            throw ce
        } catch (io: IOException) {
            Timber.w(io, "Network failure during reverse geocode")
            Result.failure(LocationError.Network(io))
        } catch (t: Throwable) {
            Timber.w(t, "Reverse geocode failed")
            Result.failure(LocationError.Unknown(t))
        }
    }
}