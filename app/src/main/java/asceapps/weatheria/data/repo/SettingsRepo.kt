package asceapps.weatheria.data.repo

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.work.*
import asceapps.weatheria.R
import asceapps.weatheria.data.model.WeatherInfo
import asceapps.weatheria.util.onChangeFlow
import asceapps.weatheria.worker.RefreshWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
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
	private val defValBool = false
	private val unitsKey = appContext.getString(R.string.key_units)
	private val locUseDevice = appContext.getString(R.string.key_loc_use_device)
	private val locAccuracyHigh = appContext.getString(R.string.key_loc_accuracy_high)
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

	// 0 = metric, 1 = imperial
	private val units: Int
		get() = (prefs.getString(unitsKey, defValStr) ?: defValStr).toInt()

	val useDeviceForLocation: Boolean
		get() = prefs.getBoolean(locUseDevice, defValBool)

	val useHighAccuracyLocation: Boolean
		get() = prefs.getBoolean(locAccuracyHigh, defValBool)

	private val autoRefreshPeriod: String
		get() = prefs.getString(autoRefreshKey, defValStr) ?: defValStr

	var selectedPos
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
		val never = autoRefreshValues[0]
		if(autoRefreshPeriod == never) {
			workManager.cancelUniqueWork(refreshWorkName)
			return
		}

		// cancel any previous different work
		autoRefreshValues.filterNot { it == never || it == autoRefreshPeriod }
			.forEach {
				workManager.cancelAllWorkByTag(it)
			}

		val periodL = autoRefreshPeriod.toLong()
		val constraints = Constraints.Builder()
			.setRequiredNetworkType(NetworkType.UNMETERED)
			.setRequiresBatteryNotLow(true)
			.build()
		val now = LocalDateTime.now()
		val lastMidnight = now.truncatedTo(ChronoUnit.DAYS)
		var nextSection = lastMidnight
		do {
			nextSection = nextSection.plusHours(periodL)
		} while(nextSection < now)
		val secondsUntilNextSection = Duration.between(now, nextSection).seconds
		val work = PeriodicWorkRequestBuilder<RefreshWorker>(periodL, TimeUnit.HOURS)
			.addTag(autoRefreshPeriod) // to cancel by tag
			.setConstraints(constraints)
			.setInitialDelay(secondsUntilNextSection, TimeUnit.SECONDS)
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