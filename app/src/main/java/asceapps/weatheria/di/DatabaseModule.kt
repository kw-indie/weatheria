package asceapps.weatheria.di

import android.content.Context
import asceapps.weatheria.db.AppDB
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
	fun provideAppDB(@ApplicationContext context: Context) = AppDB.build(context)

	@Provides
	fun provideWeatherInfoDao(appDB: AppDB) = appDB.weatherInfoDao()
}