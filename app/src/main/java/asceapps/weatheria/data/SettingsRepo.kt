package asceapps.weatheria.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import asceapps.weatheria.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepo @Inject constructor(
	@ApplicationContext context: Context,
	val prefs: SharedPreferences
) {

	private val defVal = 0
	private val defValStr = context.getString(R.string.def_value)
	private val unitsKey = context.getString(R.string.key_units)
	private val themeKey = context.getString(R.string.key_theme)
	private val autoRefreshKey = context.getString(R.string.key_auto_refresh)
	private val selectedLocKey = "selected"

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