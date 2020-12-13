package asceapps.weatheria.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
class AppModule {

	@Singleton
	@Provides
	fun providePrefs(@ApplicationContext context: Context): SharedPreferences =
		PreferenceManager.getDefaultSharedPreferences(context)
}