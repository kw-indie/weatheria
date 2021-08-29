package asceapps.weatheria.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import asceapps.weatheria.R
import asceapps.weatheria.databinding.ActivityMainBinding
import asceapps.weatheria.util.edgeToEdge
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity: AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)

		WindowCompat.setDecorFitsSystemWindows(window, false)
		binding.navHost.edgeToEdge(statusBar = false)
		binding.appbar.edgeToEdge(navBarPortrait = false)

		// set up navigation
		setSupportActionBar(binding.toolbar)
		// used this instead of findNavController because that one requires views to be laid out already
		// i.e. can be used after Activity.onCreate or inside fragment
		val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
		NavigationUI.setupActionBarWithNavController(this, navHostFragment.navController)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.main_menu, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when(item.itemId) {
			R.id.settingsFragment -> findNavController(R.id.nav_host).navigate(R.id.settingsFragment)
			else -> return super.onOptionsItemSelected(item)
		}
		return true
	}

	override fun onSupportNavigateUp(): Boolean {
		return findNavController(R.id.nav_host).navigateUp()
	}
}