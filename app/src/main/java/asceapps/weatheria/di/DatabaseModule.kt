package asceapps.weatheria.di

import android.content.Context
import androidx.room.Room
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
	fun provideAppDB(@ApplicationContext appContext: Context) = Room.databaseBuilder(
		appContext, AppDatabase::class.java, AppDatabase.DB_NAME
	).build()

	@Provides
	@Singleton
	fun provideWeatherInfoDao(appDatabase: AppDatabase) = appDatabase.weatherInfoDao()
}