package asceapps.weatheria

import android.app.Application
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App: Application(), Configuration.Provider {

	override fun getWorkManagerConfiguration() =
		Configuration.Builder()
			//.setMinimumLoggingLevel(Log.INFO)
			.build()
}