package bose.ankush.weatherify.di

import android.content.Context
import bose.ankush.weatherify.base.location.DeviceLocationClient
import bose.ankush.weatherify.base.location.LocationClient
import bose.ankush.weatherify.di.qualifiers.DeviceLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    @Singleton
    @Provides
    @DeviceLocation
    fun provideFusedLocationProviderClient(context: Context): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @Singleton
    @Provides
    @DeviceLocation
    fun providesLocationClient(
        context: Context,
        client: FusedLocationProviderClient
    ) : LocationClient =
        DeviceLocationClient(context, client)
}
