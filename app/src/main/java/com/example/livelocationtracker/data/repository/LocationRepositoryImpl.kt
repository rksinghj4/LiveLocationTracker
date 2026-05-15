package com.example.livelocationtracker.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.livelocationtracker.domain.model.LocationError
import com.example.livelocationtracker.domain.model.UserLocation
import com.example.livelocationtracker.domain.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationProviderClient: FusedLocationProviderClient,
) : LocationRepository {
    //In this method nothing to offload on IO. Just registering and Unregistering the LocationCallback
    @SuppressLint("MissingPermission")
    override fun observeLocationUpdates(intervalMillis: Long): Flow<UserLocation> = callbackFlow {
        if (!hasLocationPermission()) {
            close(LocationError.PermissionDenied)
            return@callbackFlow
        }
        if (!isLocationEnabled()) {
            close(LocationError.LocationDisabled)
            return@callbackFlow
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    Timber.d("onLocationResult callBack @%s", loc)

                    trySend(
                        UserLocation(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else 0f,
                            timestampMillis = loc.time,
                        )
                    )
                }
            }
        }

        Timber.d("Starting location updates @%dms", intervalMillis)
        try {
            //Register the callback with Google play service
            fusedLocationProviderClient.requestLocationUpdates(request, callback, /* looper = */ null)
                .addOnFailureListener { e ->
                    Timber.e(e, "requestLocationUpdates failed")
                    close(LocationError.Unknown(e))
                }
        } catch (se: SecurityException) {
            close(LocationError.PermissionDenied)
            return@callbackFlow
        }

        awaitClose {//It's mandatory to avoid the memory leak
            Timber.d("Stopping location updates")
            //Remove listener
            fusedLocationProviderClient.removeLocationUpdates(callback)
        }
    }

    override fun isLocationEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
}