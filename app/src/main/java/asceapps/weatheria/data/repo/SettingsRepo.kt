package asceapps.weatheria.data.repo

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.work.*
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
	private val defValStr = appContext.getString(R.string.def_value)
	private val unitsKey = appContext.getString(R.string.key_units)
	private val speedUnits = appContext.resources.getStringArray(R.array.units_speed)
	private val autoRefreshKey = appContext.getString(R.string.key_auto_refresh)
	private val autoRefreshValues = appContext.resources.getStringArray(R.array.values_auto_refresh)
	private val selectedLocKey = "selected"

	val changesFlow = prefs.onChangeFlow()

	// 0 = metric, 1 = imperial
	private val units: Int
		get() = (prefs.getString(unitsKey, defValStr) ?: defValStr).toInt()

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