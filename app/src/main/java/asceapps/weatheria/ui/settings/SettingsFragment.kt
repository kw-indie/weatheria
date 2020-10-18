package asceapps.weatheria.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import asceapps.weatheria.R

class SettingsFragment: PreferenceFragmentCompat() {

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(R.xml.root_preferences, rootKey)
	}
}