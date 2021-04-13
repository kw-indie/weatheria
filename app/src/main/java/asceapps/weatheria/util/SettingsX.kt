package asceapps.weatheria.util

import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun SharedPreferences.onChangeFlow() = callbackFlow<String> {
	val changeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
		try {
			offer(key)
		} catch(e: Exception) {
			close(e)
		}
	}
	registerOnSharedPreferenceChangeListener(changeListener)

	awaitClose {
		unregisterOnSharedPreferenceChangeListener(changeListener)
	}
}