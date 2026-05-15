package com.example.livelocationtracker.di

import com.example.livelocationtracker.data.repository.GeocodingRepositoryImpl
import com.example.livelocationtracker.data.repository.LocationRepositoryImpl
import com.example.livelocationtracker.domain.repository.GeocodingRepository
import com.example.livelocationtracker.domain.repository.LocationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds
    @Singleton
    abstract fun bindGeocodingRepository(impl: GeocodingRepositoryImpl): GeocodingRepository
}