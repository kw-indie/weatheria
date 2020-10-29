package asceapps.weatheria.ui.settings

import android.os.Bundle
import androidx.datastore.preferences.edit
import androidx.datastore.preferences.preferencesKey
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import asceapps.weatheria.R
import asceapps.weatheria.ui.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SettingsFragment: PreferenceFragmentCompat() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		preferenceManager.preferenceDataStore = object: PreferenceDataStore() {
			private val dataStore = (requireActivity() as MainActivity).dataStore
			override fun putString(key: String?, value: String?) {
				if(key != null)
					lifecycleScope.launch {
						dataStore.edit {prefs ->
							value?.let {prefs[preferencesKey<String>(key)] = it}
						}
					}
			}

			override fun getString(key: String?, defValue: String?): String? {
				return runBlocking {
					if(key != null)
						dataStore.data.map {prefs ->
							prefs[preferencesKey<String>(key)] ?: defValue
						}.first()
					else defValue
				}
			}
		}
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(R.xml.root_preferences, rootKey)
	}
}