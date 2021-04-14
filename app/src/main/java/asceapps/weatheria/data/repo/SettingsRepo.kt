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
import asceapps.weatheria.util.onChangeFlow
import asceapps.weatheria.worker.AutoRefreshWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ActivityRetainedScoped
class SettingsRepo @Inject constructor(
	@ApplicationContext appContext: Context,
	private val prefs: SharedPreferences
) {

	private val workManager = WorkManager.getInstance(appContext)
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

	val changesFlow = prefs.onChangeFlow()

	// 0 = metric, 1 = imperial
	private val units: Int
		get() = (prefs.getString(unitsKey, defValStr) ?: defValStr).toInt()

	val useDeviceForLocation: Boolean
		get() = prefs.getBoolean(locUseDevice, defValBool)

	val isLocationAccuracyHigh: Boolean
		get() = prefs.getBoolean(locAccuracyHigh, defValBool)

	private val autoRefreshPeriod: String
		get() = prefs.getString(autoRefreshKey, defValStr) ?: defValStr

	val isMetric: Boolean
		get() = units == 0

	val speedUnit: String
		get() = speedUnits[units]

	var selectedLocation = prefs.getInt(selectedLocKey, defVal)
		get() = prefs.getInt(selectedLocKey, defVal)
		set(value) {
			if (field != value) {
				field = value
				prefs.edit { putInt(selectedLocKey, value) }
			}
		}

	fun update(key: String) {
		when (key) {
			autoRefreshKey -> updateAutoRefresh()
		}
	}

	private fun updateAutoRefresh() {
		val autoRefreshWorkName = AutoRefreshWorker::class.qualifiedName!!
		val never = autoRefreshValues[0]
		if (autoRefreshPeriod == never) {
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
		val work = PeriodicWorkRequestBuilder<AutoRefreshWorker>(periodL, TimeUnit.HOURS)
			.addTag(autoRefreshPeriod)
			.setConstraints(constraints)
			.setInitialDelay(periodL, TimeUnit.HOURS)
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