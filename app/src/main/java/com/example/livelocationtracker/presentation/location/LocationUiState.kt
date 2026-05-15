package com.example.livelocationtracker.presentation.location

import com.example.livelocationtracker.domain.model.Address
import com.example.livelocationtracker.domain.model.UserLocation

/**
 * UI shape rendered by [LocationScreen]. Single state object — easier to drive
 * loading/refresh states than parallel flows.
 */
data class LocationUiState(
    val phase: Phase = Phase.Loading,
    val location: UserLocation? = null,
    val address: Address? = null,
    val addressLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val locationServicesDisabled: Boolean = false,
) {
    sealed interface Phase {
        data object Loading : Phase
        data object Success : Phase
        data class Error(val message: String) : Phase
    }
}