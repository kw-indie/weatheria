package asceapps.weatheria.di

import android.content.Context
import asceapps.weatheria.data.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
class DatabaseModule {

	@Singleton
	@Provides
	fun provideAppDB(@ApplicationContext context: Context) = AppDatabase.build(context)

	@Provides
	fun provideWeatherInfoDao(appDatabase: AppDatabase) = appDatabase.weatherInfoDao()
}