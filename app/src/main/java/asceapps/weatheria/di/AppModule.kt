package asceapps.weatheria.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(ActivityRetainedComponent::class)
class AppModule {

	@Provides
	fun provideSharedPrefs(@ApplicationContext appContext: Context): SharedPreferences =
		PreferenceManager.getDefaultSharedPreferences(appContext)
}