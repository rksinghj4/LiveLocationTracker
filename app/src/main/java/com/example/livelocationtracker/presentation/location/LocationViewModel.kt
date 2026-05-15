package com.example.livelocationtracker.presentation.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livelocationtracker.domain.model.LocationError
import com.example.livelocationtracker.domain.model.UserLocation
import com.example.livelocationtracker.domain.usecase.GetAddressFromLocationUseCase
import com.example.livelocationtracker.domain.usecase.GetLiveLocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val getLiveLocation: GetLiveLocationUseCase,
    private val getAddress: GetAddressFromLocationUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LocationUiState())
    val state: StateFlow<LocationUiState> = _state.asStateFlow()

    /** One-shot UI events that don't belong in persistent state (snackbars, etc.). */
    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var collectionJob: Job? = null
    private var lastGeocodedLat: Double? = null
    private var lastGeocodedLng: Double? = null

    /**
     * Start streaming location updates. Idempotent; calling again is a no-op while a
     * collection is already active. Pass `permissionGranted = false` to surface a
     * permission-denied state without attempting to start the provider.
     */
    fun start(permissionGranted: Boolean) {
        if (!permissionGranted) {
            _state.update {
                it.copy(
                    phase = LocationUiState.Phase.Error("Location permission required"),
                    errorMessage = "Location permission required",
                )
            }
            return
        }
        if (collectionJob?.isActive == true) return

        if (!getLiveLocation.isLocationEnabled()) {
            _state.update {
                it.copy(
                    phase = LocationUiState.Phase.Error("Location services are disabled"),
                    locationServicesDisabled = true,
                )
            }
            return
        }
        _state.update { it.copy(locationServicesDisabled = false, errorMessage = null) }

        collectionJob = viewModelScope.launch {
            getLiveLocation()
                .catch { e ->
                    Timber.w(e, "Location stream error")
                    handleStreamError(e)
                }
                .collect { loc -> onNewLocation(loc) }
        }
    }

    /** Stop the underlying provider. Called from the lifecycle observer when the screen pauses. */
    fun stop() {
        collectionJob?.cancel()
        collectionJob = null
    }

    fun refresh(permissionGranted: Boolean) {
        _state.update { it.copy(isRefreshing = true) }
        // Force a fresh geocode on the next fix.
        lastGeocodedLat = null
        lastGeocodedLng = null
        stop()
        start(permissionGranted)
    }

    private fun onNewLocation(loc: UserLocation) {
        _state.update {
            it.copy(
                phase = LocationUiState.Phase.Success,
                location = loc,
                isRefreshing = false,
                errorMessage = null,
                locationServicesDisabled = false,
            )
        }
        maybeReverseGeocode(loc)
    }

    private fun maybeReverseGeocode(loc: UserLocation) {
        // Only re-geocode when the user has moved meaningfully (~ >55m). Nominatim's usage
        // policy caps us at 1 req/sec; this keeps us well under that even at high update rates.
        val moved = lastGeocodedLat == null ||
            abs(loc.latitude - lastGeocodedLat!!) > 0.0005 ||
            abs(loc.longitude - lastGeocodedLng!!) > 0.0005
        if (!moved) return

        lastGeocodedLat = loc.latitude
        lastGeocodedLng = loc.longitude

        viewModelScope.launch {
            _state.update { it.copy(addressLoading = true) }
            val result = getAddress(loc.latitude, loc.longitude)
            result
                .onSuccess { addr ->
                    _state.update { it.copy(address = addr, addressLoading = false) }
                }
                .onFailure { e ->
                    Timber.w(e, "Reverse geocode failed")
                    _state.update { it.copy(addressLoading = false) }
                    _events.trySend(Event.ShowMessage("Couldn't fetch address: ${e.message ?: "network error"}"))
                }
        }
    }

    private fun handleStreamError(error: Throwable) {
        when (error) {
            is LocationError.PermissionDenied -> _state.update {
                it.copy(
                    phase = LocationUiState.Phase.Error("Location permission required"),
                    isRefreshing = false,
                )
            }

            is LocationError.LocationDisabled -> _state.update {
                it.copy(
                    phase = LocationUiState.Phase.Error("Location services are disabled"),
                    locationServicesDisabled = true,
                    isRefreshing = false,
                )
            }

            else -> {
                val msg = error.message ?: "Unknown error"
                _state.update {
                    it.copy(
                        phase = LocationUiState.Phase.Error(msg),
                        errorMessage = msg,
                        isRefreshing = false,
                    )
                }
                _events.trySend(Event.ShowMessage(msg))
            }
        }
    }

    sealed interface Event {
        data class ShowMessage(val text: String) : Event
    }
}