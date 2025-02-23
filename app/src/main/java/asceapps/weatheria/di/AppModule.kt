package asceapps.weatheria.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.work.WorkManager
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

	@Provides
	fun provideSharedPrefs(@ApplicationContext appContext: Context): SharedPreferences =
		PreferenceManager.getDefaultSharedPreferences(appContext)

	@Provides
	fun provideWorkManager(@ApplicationContext appContext: Context): WorkManager =
		WorkManager.getInstance(appContext)

	@Provides
	fun provideSplitInstallManager(@ApplicationContext appContext: Context): SplitInstallManager =
		SplitInstallManagerFactory.create(appContext)
}