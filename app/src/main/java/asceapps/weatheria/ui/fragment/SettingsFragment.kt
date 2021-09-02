package asceapps.weatheria.ui.fragment

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.Preference.SummaryProvider
import androidx.preference.PreferenceFragmentCompat
import asceapps.weatheria.R
import asceapps.weatheria.data.repo.SettingsRepo
import asceapps.weatheria.ext.observe
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment: PreferenceFragmentCompat() {

	@Inject
	lateinit var repo: SettingsRepo

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(R.xml.root_preferences, rootKey)

		val locProviderPref = findPreference<ListPreference>(getString(R.string.key_loc_provider))
		val locProviderSummaries = resources.getStringArray(R.array.summaries_loc_provider)
		locProviderPref?.summaryProvider = SummaryProvider<ListPreference> { pref ->
			val index = pref.value.toInt()
			locProviderSummaries[index].format(pref.entry)
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		repo.changesFlow.observe(viewLifecycleOwner) { key ->
			repo.update(key)
		}
	}

	override fun onPrepareOptionsMenu(menu: Menu) {
		super.onPrepareOptionsMenu(menu)
		menu.findItem(R.id.settingsFragment).isVisible = false
	}
}