package asceapps.weatheria.di

import android.content.Context
import asceapps.weatheria.data.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {

	@Provides
	@Singleton
	fun provideAppDB(@ApplicationContext context: Context) = AppDatabase.build(context)

	@Provides
	fun provideLocationDao(appDatabase: AppDatabase) = appDatabase.locationDao()

	@Provides
	fun provideWeatherInfoDao(appDatabase: AppDatabase) = appDatabase.weatherInfoDao()
}