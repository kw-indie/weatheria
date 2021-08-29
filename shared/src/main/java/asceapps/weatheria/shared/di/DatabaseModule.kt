package asceapps.weatheria.shared.di

import android.content.Context
import asceapps.weatheria.shared.data.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class DatabaseModule {

	@Provides
	@Singleton
	fun provideAppDB(@ApplicationContext appContext: Context) = AppDatabase.create(appContext)

	@Provides
	@Singleton
	fun provideWeatherInfoDao(appDatabase: AppDatabase) = appDatabase.weatherInfoDao()

	@Provides
	@IoDispatcher
	fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}