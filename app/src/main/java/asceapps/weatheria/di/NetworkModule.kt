package asceapps.weatheria.di

import android.content.Context
import asceapps.weatheria.data.WeatherService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
class NetworkModule {

	@Singleton
	@Provides
	fun provideWeatherService(@ApplicationContext context: Context) = WeatherService.create(context)
}