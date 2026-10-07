package com.bramwel.eccomerceapp.features.profile.di

import com.bramwel.eccomerceapp.features.profile.data.repository.ProfileRepositoryImpl
import com.bramwel.eccomerceapp.features.profile.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {
    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
}
