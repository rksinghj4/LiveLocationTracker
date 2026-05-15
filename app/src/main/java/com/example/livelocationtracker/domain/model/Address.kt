package com.example.livelocationtracker.domain.model

data class Address(
    val displayName: String,
    val road: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    val postcode: String?,
)