package asceapps.weatheria

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.android.play.core.splitcompat.SplitCompat
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App: Application(), Configuration.Provider {

	@Inject
	lateinit var workerFactory: HiltWorkerFactory

	override fun getWorkManagerConfiguration() =
		Configuration.Builder()
			.setWorkerFactory(workerFactory)
			//.setMinimumLoggingLevel(Log.INFO)
			.build()

	override fun attachBaseContext(base: Context?) {
		super.attachBaseContext(base)
		SplitCompat.install(this)
	}
}