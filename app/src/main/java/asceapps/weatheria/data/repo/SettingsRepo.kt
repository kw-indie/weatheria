package asceapps.weatheria.data.repo

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import asceapps.weatheria.R
import asceapps.weatheria.ext.onChangeFlow
import asceapps.weatheria.util.Formatter
import asceapps.weatheria.worker.RefreshWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// todo move to shared
@Singleton
class SettingsRepo @Inject constructor(
	@ApplicationContext appContext: Context,
	private val prefs: SharedPreferences,
	private val workManager: WorkManager
) {

	companion object {
		private const val defVal = 0
		private const val defValStr = "0"

		const val LOCATION_PROVIDER_IP = 0
		const val LOCATION_PROVIDER_NETWORK = 1
		const val LOCATION_PROVIDER_ALL = 2

		const val AUTO_REFRESH_NEVER = 0
		const val AUTO_REFRESH_ONCE = 1
		const val AUTO_REFRESH_TWICE = 2
	}

	private val unitsKey = appContext.getString(R.string.key_units)
	private val locProviderKey = appContext.getString(R.string.key_loc_provider)
	private val autoRefreshKey = appContext.getString(R.string.key_auto_refresh)
	private val selectedKey = appContext.getString(R.string.key_selected)

	init {
		// on reinstall or sth, make sure all settings are reapplied, eg. refreshWorker.
		// irl, this happens when app settings are auto backed up
		updateUnits()
		updateAutoRefresh()
	}

	val changesFlow get() = prefs.onChangeFlow()

	private val units: Int
		get() = prefs.getString(unitsKey, defValStr)!!.toInt()

	private val locationProvider: Int
		get() = prefs.getString(locProviderKey, defValStr)!!.toInt()
	val useDeviceForLocation: Boolean
		get() = locationProvider != LOCATION_PROVIDER_IP
	val useHighAccuracyLocation: Boolean
		get() = locationProvider == LOCATION_PROVIDER_ALL

	private val autoRefresh: Int
		get() = prefs.getString(autoRefreshKey, defValStr)!!.toInt()

	var selectedPos: Int
		get() = prefs.getInt(selectedKey, defVal)
		set(value) {
			prefs.edit { putInt(selectedKey, value) }
		}

	fun update(key: String) {
		when(key) {
			unitsKey -> updateUnits()
			autoRefreshKey -> updateAutoRefresh()
		}
	}

	private fun updateUnits() {
		Formatter.unitSystem = units
	}

	private fun updateAutoRefresh() {
		val workName = RefreshWorker::class.qualifiedName!!
		val period = when(autoRefresh) {
			AUTO_REFRESH_NEVER -> {
				workManager.cancelUniqueWork(workName)
				return
			}
			AUTO_REFRESH_ONCE -> {
				workManager.cancelAllWorkByTag(AUTO_REFRESH_TWICE.toString())
				24L
			}
			else -> { // twice a day
				workManager.cancelAllWorkByTag(AUTO_REFRESH_ONCE.toString())
				12L
			}
		}
		// About constraints: leave decision to user (who can disable auto updates)
		// NetworkType.UNMETERED: let's be honest, nobody cares about a few kb's even on data
		// BatteryNotLow: this process takes way too little energy to care about low battery

		// don't start for all users at midnight or some other $h!t
		// let users sync at random times so we don't hit api limits
		val work = PeriodicWorkRequestBuilder<RefreshWorker>(period, TimeUnit.HOURS)
			// if we don't set a delay, it will run once immediately
			.setInitialDelay(period, TimeUnit.HOURS)
			// to cancel by tag
			.addTag(period.toString())
			.build()
		workManager.enqueueUniquePeriodicWork(
			workName,
			ExistingPeriodicWorkPolicy.KEEP,
			work
		)
		// debugging commands:
		// adb shell dumpsys jobscheduler
		// adb shell am broadcast -a "androidx.work.diagnostics.REQUEST_DIAGNOSTICS" -p "<package_name>"
	}
}