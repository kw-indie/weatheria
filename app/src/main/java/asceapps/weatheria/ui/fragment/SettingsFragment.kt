package asceapps.weatheria.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceFragmentCompat
import asceapps.weatheria.R
import asceapps.weatheria.data.repo.SettingsRepo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

		repo.changesFlow.onEach { key ->
			repo.update(key)
		}.launchIn(viewLifecycleOwner.lifecycleScope)
	}
}