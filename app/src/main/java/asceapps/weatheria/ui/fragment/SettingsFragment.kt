package asceapps.weatheria.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
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
		view.findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener {
			findNavController().navigateUp()
		}
		val unitsKey = requireContext().getString(R.string.key_units)
		val themeKey = requireContext().getString(R.string.key_theme)
		val autoRefreshKey = requireContext().getString(R.string.key_auto_refresh)
		repo.changes.observe(viewLifecycleOwner) {
			when(it) {
				unitsKey -> updateUnits()
				themeKey -> updateTheme()
				autoRefreshKey -> updateAutoRefresh()
			}
		}
	}

	private fun updateUnits() {
		// todo setMetric
	}

	private fun updateTheme() {
		AppCompatDelegate.setDefaultNightMode(
			when(repo.theme) {
				0 -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
				1 -> AppCompatDelegate.MODE_NIGHT_YES
				else -> AppCompatDelegate.MODE_NIGHT_NO
			}
		)
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
	}
}