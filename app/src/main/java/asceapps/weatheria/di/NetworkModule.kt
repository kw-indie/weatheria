package asceapps.weatheria.di

import android.content.Context
import asceapps.weatheria.data.api.GeoIPService
import asceapps.weatheria.data.api.WeatherService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

	@Provides
	@Singleton
	fun provideWeatherService(@ActivityContext context: Context) = WeatherService.create(context)
}