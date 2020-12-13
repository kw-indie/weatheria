package asceapps.weatheria.data

import android.content.Context
import android.content.SharedPreferences
import asceapps.weatheria.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepo @Inject constructor(
	@ApplicationContext context: Context,
	val prefs: SharedPreferences
) {

	private val defVal = context.getString(R.string.def_value)
	private val unitsKey = context.getString(R.string.key_units)
	private val themeKey = context.getString(R.string.key_theme)
	private val autoRefreshKey = context.getString(R.string.key_auto_refresh)

	val units: Int
		get() = (prefs.getString(unitsKey, defVal) ?: defVal).toInt()

	val theme: Int
		get() = (prefs.getString(themeKey, defVal) ?: defVal).toInt()

	val autoRefresh: Int
		get() = (prefs.getString(autoRefreshKey, defVal) ?: defVal).toInt()
}