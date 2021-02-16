package asceapps.weatheria.data.repo

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.asLiveData
import androidx.preference.PreferenceManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import asceapps.weatheria.R
import asceapps.weatheria.worker.AutoRefreshWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ActivityRetainedScoped
class SettingsRepo @Inject constructor(@ApplicationContext context: Context) {

	private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
	private val workManager = WorkManager.getInstance(context)
	private val defVal = 0
	private val defValStr = context.getString(R.string.def_value)
	private val unitsKey = context.getString(R.string.key_units)
	private val speedUnits = context.resources.getStringArray(R.array.units_speed)
	private val autoRefreshKey = context.getString(R.string.key_auto_refresh)
	private val selectedLocKey = "selected"

	val changes = callbackFlow<String> {
		val changeListener = SharedPreferences.OnSharedPreferenceChangeListener {_, key ->
			try {
				offer(key)
			} catch(e: Exception) {
				close(e)
			}
		}
		prefs.registerOnSharedPreferenceChangeListener(changeListener)

		awaitClose {
			prefs.unregisterOnSharedPreferenceChangeListener(changeListener)
		}
	}.asLiveData()

	private val units: Int
		get() = (prefs.getString(unitsKey, defValStr) ?: defValStr).toInt()

	private val autoRefresh: Int
		get() = (prefs.getString(autoRefreshKey, defValStr) ?: defValStr).toInt()

	val metric: Boolean
		get() = units == 0

	val speedUnit: String
		get() = speedUnits[units]

	var selectedLocation = prefs.getInt(selectedLocKey, defVal)
		get() = prefs.getInt(selectedLocKey, defVal)
		set(value) {
			if(field != value) {
				field = value
				prefs.edit {putInt(selectedLocKey, value)}
			}
		}

	fun reapply() {
		updateAutoRefresh()
	}

	fun updateAutoRefresh() {
		val autoRefreshWorkName = "autoRefreshWork"
		val duration: Long = when(autoRefresh) {
			0 -> { // never
				workManager.cancelUniqueWork(autoRefreshWorkName)
				return
			}
			1 -> {
				// cancel all other work
				workManager.cancelAllWorkByTag("12")
				workManager.cancelAllWorkByTag("24")
				6
			}
			2 -> {
				workManager.cancelAllWorkByTag("6")
				workManager.cancelAllWorkByTag("24")
				12
			}
			else -> {
				workManager.cancelAllWorkByTag("6")
				workManager.cancelAllWorkByTag("12")
				24
			}
		}
		val constraints = Constraints.Builder()
			.setRequiredNetworkType(NetworkType.UNMETERED)
			.setRequiresBatteryNotLow(true)
			.build()
		val work = PeriodicWorkRequestBuilder<AutoRefreshWorker>(duration, TimeUnit.HOURS)
			.addTag("$duration")
			.setConstraints(constraints)
			.setInitialDelay(duration, TimeUnit.HOURS)
			.build()
		workManager.enqueueUniquePeriodicWork(
			autoRefreshWorkName,
			ExistingPeriodicWorkPolicy.KEEP,
			work
		)
		// debugging commands:
		// adb shell dumpsys jobscheduler
		// adb shell am broadcast -a "androidx.work.diagnostics.REQUEST_DIAGNOSTICS" -p "<package_name>"
	}
}