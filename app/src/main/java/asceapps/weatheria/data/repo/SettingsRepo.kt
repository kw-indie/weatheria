package asceapps.weatheria.data.repo

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.asLiveData
import androidx.preference.PreferenceManager
import asceapps.weatheria.R
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

@ActivityRetainedScoped
class SettingsRepo @Inject constructor(@ApplicationContext context: Context) {

	private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
	private val defVal = 0
	private val defValStr = context.getString(R.string.def_value)
	private val unitsKey = context.getString(R.string.key_units)
	private val themeKey = context.getString(R.string.key_theme)
	private val autoRefreshKey = context.getString(R.string.key_auto_refresh)
	private val selectedLocKey = "selected"

	val changes = callbackFlow<String> {
		val changeListener = SharedPreferences.OnSharedPreferenceChangeListener {_, key ->
			try {
				offer(key)
			} catch(t: Throwable) {
				t.printStackTrace() // offer() failed
			}
		}
		prefs.registerOnSharedPreferenceChangeListener(changeListener)

		awaitClose {
			prefs.unregisterOnSharedPreferenceChangeListener(changeListener)
		}
	}.asLiveData()

	val units: Int
		get() = (prefs.getString(unitsKey, defValStr) ?: defValStr).toInt()

	val theme: Int
		get() = (prefs.getString(themeKey, defValStr) ?: defValStr).toInt()

	val autoRefresh: Int
		get() = (prefs.getString(autoRefreshKey, defValStr) ?: defValStr).toInt()

	var selectedLocation = prefs.getInt(selectedLocKey, defVal)
		get() = prefs.getInt(selectedLocKey, defVal)
		set(value) {
			if(field != value) {
				field = value
				prefs.edit {putInt(selectedLocKey, value)}
			}
		}
}