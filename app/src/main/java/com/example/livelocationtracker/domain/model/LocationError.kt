package com.example.livelocationtracker.domain.model

/**
 * Domain-level errors. Keeps the presentation layer free of Android/Retrofit types.
 */
sealed class LocationError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    object PermissionDenied : LocationError("Location permission not granted")
    object LocationDisabled : LocationError("Device location services are disabled")
    class Unknown(cause: Throwable) : LocationError(cause.message ?: "Unknown location error", cause)
    class Network(cause: Throwable) : LocationError(cause.message ?: "Network error", cause)
}