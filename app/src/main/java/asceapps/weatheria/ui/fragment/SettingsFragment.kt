package asceapps.weatheria.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceFragmentCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import asceapps.weatheria.R
import asceapps.weatheria.data.repo.SettingsRepo
import asceapps.weatheria.worker.AutoRefreshWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment: PreferenceFragmentCompat() {

	@Inject
	lateinit var repo: SettingsRepo

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(R.xml.root_preferences, rootKey)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val unitsKey = requireContext().getString(R.string.key_units)
		val autoRefreshKey = requireContext().getString(R.string.key_auto_refresh)
		repo.changes.observe(viewLifecycleOwner) {
			when(it) {
				unitsKey -> updateUnits()
				autoRefreshKey -> updateAutoRefresh()
			}
		}
	}

	private fun updateUnits() {
		// todo setMetric
	}

	private fun updateAutoRefresh() {
		val workManager = WorkManager.getInstance(requireContext())
		val autoRefreshWorkName = "autoRefreshWork"
		val duration: Long = when(repo.autoRefresh) {
			0 -> 0 // never
			1 -> 6
			2 -> 12
			else -> 24
		}
		if(duration == 0L) {
			workManager.cancelUniqueWork(autoRefreshWorkName)
		} else {
			// since this method is only called when a change happens to the setting
			// we don't need to check if existing work is similar, always replace
			val constraints = Constraints.Builder()
				.setRequiredNetworkType(NetworkType.UNMETERED)
				.setRequiresBatteryNotLow(true)
				.build()
			val work = PeriodicWorkRequestBuilder<AutoRefreshWorker>(duration, TimeUnit.HOURS)
				.setConstraints(constraints)
				.setInitialDelay(duration, TimeUnit.HOURS)
				.build()
			workManager.enqueueUniquePeriodicWork(
				autoRefreshWorkName,
				ExistingPeriodicWorkPolicy.REPLACE,
				work
			)
		}
		// debugging commands:
		// adb shell dumpsys jobscheduler
		// adb shell am broadcast -a "androidx.work.diagnostics.REQUEST_DIAGNOSTICS" -p "<package_name>"
	}
}