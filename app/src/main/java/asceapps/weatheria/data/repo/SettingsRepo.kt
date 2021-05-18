package asceapps.weatheria.data.repo

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import asceapps.weatheria.R
import asceapps.weatheria.data.model.WeatherInfo
import asceapps.weatheria.util.onChangeFlow
import asceapps.weatheria.worker.AutoRefreshWorker
import asceapps.weatheria.worker.DatabasePruneWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ActivityRetainedScoped
class SettingsRepo @Inject constructor(
	@ApplicationContext appContext: Context,
	private val prefs: SharedPreferences,
	private val workManager: WorkManager
) {

	private val defVal = 0
	private val defValStr = "0"
	private val defValBool = false
	private val unitsKey = appContext.getString(R.string.key_units)
	private val speedUnits = appContext.resources.getStringArray(R.array.units_speed)
	private val locUseDevice = appContext.getString(R.string.key_loc_use_device)
	private val locAccuracyHigh = appContext.getString(R.string.key_loc_accuracy_high)
	private val autoRefreshKey = appContext.getString(R.string.key_auto_refresh)
	private val autoRefreshValues = appContext.resources.getStringArray(R.array.values_auto_refresh)
	private val selectedLocKey = "selected"

	init {
		// on reinstall or sth, make sure all settings are reapplied, eg. refreshWorker.
		// irl, this happens when app settings are auto backed up
		updateUnits()
		updateAutoRefresh()
		updatePruneWorker()
	}

	val changesFlow get() = prefs.onChangeFlow()

	// 0 = metric, 1 = imperial
	private val units: Int
		get() = (prefs.getString(unitsKey, defValStr) ?: defValStr).toInt()

	private val isMetric: Boolean
		get() = units == 0

	private val speedUnit: String
		get() = speedUnits[units]

	val useDeviceForLocation: Boolean
		get() = prefs.getBoolean(locUseDevice, defValBool)

	val useHighAccuracyLocation: Boolean
		get() = prefs.getBoolean(locAccuracyHigh, defValBool)

	private val autoRefreshPeriod: String
		get() = prefs.getString(autoRefreshKey, defValStr) ?: defValStr

	var selectedLocation
		get() = prefs.getInt(selectedLocKey, defVal)
		set(value) {
			prefs.edit { putInt(selectedLocKey, value) }
		}

	fun update(key: String) {
		when(key) {
			unitsKey -> updateUnits()
			autoRefreshKey -> updateAutoRefresh()
		}
	}

	private fun updateUnits() {
		WeatherInfo.setFormatSystem(isMetric, speedUnit)
	}

	private fun updateAutoRefresh() {
		val autoRefreshWorkName = AutoRefreshWorker::class.qualifiedName!!
		val never = autoRefreshValues[0]
		if(autoRefreshPeriod == never) {
			workManager.cancelUniqueWork(autoRefreshWorkName)
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
		val work = PeriodicWorkRequestBuilder<AutoRefreshWorker>(periodL, TimeUnit.HOURS)
			.addTag(autoRefreshPeriod) // to cancel by tag
			.setConstraints(constraints)
			.setInitialDelay(secondsUntilNextSection, TimeUnit.SECONDS)
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

	private fun updatePruneWorker() {
		val pruneWorkName = DatabasePruneWorker::class.qualifiedName!!
		val now = LocalDateTime.now()
		val nextMidnight = now.truncatedTo(ChronoUnit.DAYS).plusDays(1)
		val secondsUntilMidnight = Duration.between(now, nextMidnight).seconds
		val work = PeriodicWorkRequestBuilder<DatabasePruneWorker>(1, TimeUnit.DAYS)
			.setInitialDelay(secondsUntilMidnight, TimeUnit.SECONDS)
			.build()
		workManager.enqueueUniquePeriodicWork(
			pruneWorkName,
			ExistingPeriodicWorkPolicy.KEEP,
			work
		)
	}
}