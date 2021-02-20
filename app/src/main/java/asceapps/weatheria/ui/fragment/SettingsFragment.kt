package asceapps.weatheria.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceFragmentCompat
import asceapps.weatheria.R
import asceapps.weatheria.data.repo.SettingsRepo
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class SettingsFragment: PreferenceFragmentCompat() {

	@Inject
	lateinit var repo: SettingsRepo

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(R.xml.root_preferences, rootKey)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val autoRefreshKey = requireContext().getString(R.string.key_auto_refresh)
		repo.changes.onEach {
			when(it) {
				autoRefreshKey -> repo.updateAutoRefresh()
			}
		}.launchIn(viewLifecycleOwner.lifecycleScope)
	}
}