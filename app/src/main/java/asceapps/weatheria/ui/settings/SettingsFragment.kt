package asceapps.weatheria.ui.settings

import android.content.Context
import android.content.SharedPreferences
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
import asceapps.weatheria.data.SettingsRepo
import dagger.hilt.android.AndroidEntryPoint
import java.time.Duration
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment: PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

	@Inject
	lateinit var repo: SettingsRepo
	private lateinit var unitsKey: String
	private lateinit var themeKey: String
	private lateinit var autoRefreshKey: String
	private val autoRefreshWorkName = "autoRefreshWork"

	override fun onAttach(context: Context) {
		super.onAttach(context)
		unitsKey = requireContext().getString(R.string.key_units)
		themeKey = requireContext().getString(R.string.key_theme)
		autoRefreshKey = requireContext().getString(R.string.key_auto_refresh)
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(R.xml.root_preferences, rootKey)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		view.findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener {
			findNavController().navigateUp()
		}
	}

	override fun onResume() {
		super.onResume()
		repo.prefs.registerOnSharedPreferenceChangeListener(this)
	}

	override fun onPause() {
		super.onPause()
		repo.prefs.unregisterOnSharedPreferenceChangeListener(this)
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
		when(key) {
			unitsKey -> updateUnits()
			themeKey -> updateTheme()
			autoRefreshKey -> updateAutoRefresh()
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
		val duration = when(repo.autoRefresh) {
			0 -> Duration.ZERO // never
			1 -> Duration.ofHours(6)
			2 -> Duration.ofHours(12)
			else -> Duration.ofDays(1)
		}
		if(duration.isZero) {
			workManager.cancelUniqueWork(autoRefreshWorkName)
		} else {
			// since this method is only called when a change happens to the setting
			// we don't need to check if existing work is similar, always replace
			val tag = duration.toString()
			val constraints = Constraints.Builder()
				.setRequiredNetworkType(NetworkType.UNMETERED)
				.setRequiresBatteryNotLow(true)
				.build()
			val work = PeriodicWorkRequestBuilder<AutoRefreshWorker>(duration)
				.setConstraints(constraints)
				.addTag(tag)
				.build()
			workManager.enqueueUniquePeriodicWork(
				autoRefreshWorkName,
				ExistingPeriodicWorkPolicy.REPLACE,
				work
			)
		}
	}
}