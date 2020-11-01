package asceapps.weatheria.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.preference.PreferenceManager
import asceapps.weatheria.R

class MainActivity: AppCompatActivity() {

	lateinit var prefs: SharedPreferences

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		window.decorView.systemUiVisibility =
			View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

		prefs = PreferenceManager.getDefaultSharedPreferences(this)
		setupNavigation()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.main_menu, menu)
		return true
	}

	override fun onSupportNavigateUp() =
		Navigation.findNavController(this, R.id.nav_host_fragment).navigateUp()

	private fun setupNavigation() {
		setSupportActionBar(findViewById(R.id.app_toolbar))
		/*used this instead of Navigation.findNavController because that one requires views to be laid out
		already, i.e. can be used after Activity.onCreate or inside fragment*/
		(supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).let {
			NavigationUI.setupActionBarWithNavController(this, it.navController)
		}
	}
}