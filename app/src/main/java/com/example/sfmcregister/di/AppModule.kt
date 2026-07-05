package com.example.sfmcregister.di

import com.example.sfmcregister.data.repository.SfmcRepository
import com.example.sfmcregister.data.repository.SfmcRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module. Mengikat interface repository ke implementasinya.
 * ContactPreferences memiliki @Inject constructor + @Singleton, jadi Hilt
 * menyediakannya otomatis tanpa @Provides.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindSfmcRepository(impl: SfmcRepositoryImpl): SfmcRepository
}
