package asceapps.weatheria.di

import android.content.Context
import asceapps.weatheria.api.WeatherService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class NetworkModule {

	@Singleton
	@Provides
	fun provideWeatherService(@ApplicationContext context: Context) = WeatherService.create(context)
}