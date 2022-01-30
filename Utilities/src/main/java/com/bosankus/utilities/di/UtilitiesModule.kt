package com.bosankus.utilities.di

import com.bosankus.utilities.DateTimeUtils
import com.bosankus.utilities.DateTimeUtilsImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilitiesModule {

    @Singleton
    @Provides
    fun provideDateTimeUtils(): DateTimeUtils = DateTimeUtilsImpl
}