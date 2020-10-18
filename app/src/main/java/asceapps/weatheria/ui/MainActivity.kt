package asceapps.weatheria.ui

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.DataStore
import androidx.datastore.preferences.Preferences
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.createDataStore
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import asceapps.weatheria.R

class MainActivity: AppCompatActivity() {

	lateinit var dataStore: DataStore<Preferences>

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		window.decorView.systemUiVisibility =
			View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

		dataStore = createDataStore(
			name = "prefs",
			migrations = listOf(SharedPreferencesMigration(applicationContext, packageName + "_preferences"))
		)
		setupNavigation()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.main_menu, menu)
		return true
	}

	override fun onSupportNavigateUp(): Boolean {
		return Navigation.findNavController(this, R.id.nav_host_fragment).navigateUp()
	}

	private fun setupNavigation() {
		setSupportActionBar(findViewById(R.id.app_toolbar))
		/*used this instead of Navigation.findNavController because that one requires views to be laid out
		already, i.e. can be used after Activity.onCreate or inside fragment*/
		(supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).let {
			NavigationUI.setupActionBarWithNavController(this, it.navController)
		}
	}
}