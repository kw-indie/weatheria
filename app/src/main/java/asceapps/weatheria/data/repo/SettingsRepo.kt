package asceapps.weatheria.data.repo

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import asceapps.weatheria.R
import asceapps.weatheria.data.model.WeatherInfo
import asceapps.weatheria.util.onChangeFlow
import asceapps.weatheria.worker.RefreshWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepo @Inject constructor(
	@ApplicationContext appContext: Context,
	private val prefs: SharedPreferences,
	private val workManager: WorkManager
) {

	private val defVal = 0
	private val defValStr = "0"
	private val unitsKey = appContext.getString(R.string.key_units)
	private val locProviderKey = appContext.getString(R.string.key_loc_provider)
	private val autoRefreshKey = appContext.getString(R.string.key_auto_refresh)
	private val autoRefreshValues = appContext.resources.getStringArray(R.array.values_auto_refresh)
	private val selectedKey = appContext.getString(R.string.key_selected)

	init {
		// on reinstall or sth, make sure all settings are reapplied, eg. refreshWorker.
		// irl, this happens when app settings are auto backed up
		updateUnits()
		updateAutoRefresh()
	}

	val changesFlow get() = prefs.onChangeFlow()

	/**
	 *  0 = metric, 1 = imperial
	 */
	private val units: Int
		get() = prefs.getString(unitsKey, defValStr)!!.toInt()

	/**
	 * 0 = IP, 1 = coarse location, 2 = fine location
	 */
	private val locProvider: Int
		get() = prefs.getString(locProviderKey, defValStr)!!.toInt()

	val useDeviceForLocation: Boolean
		get() = locProvider > 0

	val useHighAccuracyLocation: Boolean
		get() = locProvider == 2

	private val autoRefreshPeriod: String
		get() = prefs.getString(autoRefreshKey, defValStr)!!

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
		WeatherInfo.setUnitsSystem(units)
	}

	private fun updateAutoRefresh() {
		val refreshWorkName = RefreshWorker::class.qualifiedName!!
		val period = autoRefreshPeriod
		val never = autoRefreshValues[0]
		if(period == never) {
			workManager.cancelUniqueWork(refreshWorkName)
			return
		}

		// cancel any previous different work
		autoRefreshValues.forEach {
			if(it != never && it != period) {
				workManager.cancelAllWorkByTag(it)
			}
		}
		// About constraints: leave decision to user (who can disable auto updates)
		// NetworkType.UNMETERED: let's be honest, nobody cares about a few kb's even on data
		// BatteryNotLow: this process takes way too little energy to care about low battery

		// don't start for all users at midnight or some other $h!t
		// let users sync at random times so we don't hit api limits
		val periodL = period.toLong()
		val work = PeriodicWorkRequestBuilder<RefreshWorker>(periodL, TimeUnit.HOURS)
			// if we don't set a delay, it will run once immediately
			.setInitialDelay(periodL, TimeUnit.HOURS)
			// to cancel by tag
			.addTag(period)
			.build()
		workManager.enqueueUniquePeriodicWork(
			refreshWorkName,
			ExistingPeriodicWorkPolicy.KEEP,
			work
		)
		// debugging commands:
		// adb shell dumpsys jobscheduler
		// adb shell am broadcast -a "androidx.work.diagnostics.REQUEST_DIAGNOSTICS" -p "<package_name>"
	}
}