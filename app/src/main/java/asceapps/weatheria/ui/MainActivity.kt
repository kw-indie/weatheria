package asceapps.weatheria.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import asceapps.weatheria.R

class MainActivity: AppCompatActivity() {

	lateinit var prefs: SharedPreferences

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		window.run {
			decorView.systemUiVisibility =
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
			statusBarColor = 0
		}

		prefs = PreferenceManager.getDefaultSharedPreferences(this)
	}
}