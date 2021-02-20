package asceapps.weatheria.data.repo

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
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
class SettingsRepo @Inject constructor(@ApplicationContext context: Context) {

	private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
	private val workManager = WorkManager.getInstance(context)
	private val defVal = 0
	private val defValStr = context.getString(R.string.def_value)
	private val unitsKey = context.getString(R.string.key_units)
	private val speedUnits = context.resources.getStringArray(R.array.units_speed)
	private val autoRefreshKey = context.getString(R.string.key_auto_refresh)
	private val autoRefreshValues = context.resources.getStringArray(R.array.auto_refresh_entry_values)
	private val selectedLocKey = "selected"

	val changes = prefs.onChangeFlow()

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

	fun updateAutoRefresh() {
		val autoRefreshWorkName = AutoRefreshWorker::class.qualifiedName!!
		if(autoRefresh == 0) { // never
			workManager.cancelUniqueWork(autoRefreshWorkName)
			return
		}

		val periods = autoRefreshValues.map {it.toInt()}
		val period = periods[autoRefresh]
		val periodL = period.toLong()

		// cancel previous work, if any
		periods.filterNot {it == 0 || it == period}
			.forEach {
				workManager.cancelAllWorkByTag(it.toString())
			}

		// enqueue new work, if not already enqueued
		val constraints = Constraints.Builder()
			.setRequiredNetworkType(NetworkType.UNMETERED)
			.setRequiresBatteryNotLow(true)
			.build()
		val work = PeriodicWorkRequestBuilder<AutoRefreshWorker>(periodL, TimeUnit.HOURS)
			.addTag(period.toString())
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