package asceapps.weatheria.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import asceapps.weatheria.R
import asceapps.weatheria.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity: AppCompatActivity() {

	private val viewModel: MainViewModel by viewModels()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		WindowCompat.setDecorFitsSystemWindows(window, false)
		window.statusBarColor = 0

		setupNavigation()

		viewModel.error.observe(this) {e ->
			Toast.makeText(
				this,
				when(e) {
					// obvious timeout (default 2.5 s)
					//is TimeoutError -> R.string.error_timed_out
					// no internet (in fact, no dns)
					//is NoConnectionError -> R.string.error_no_internet
					// server didn't like the request for some reason
					//is ServerError -> R.string.error_server_error
					// failed to parse response string to json
					//is ParseError -> R.string.error_parsing_resp
					else -> R.string.error_unknown
				},
				Toast.LENGTH_LONG
			).show()
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.main_menu, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when(item.itemId) {
			R.id.action_settings -> findNavController(this, R.id.nav_host).navigate(R.id.action_open_settings)
			else -> return super.onOptionsItemSelected(item)
		}
		return true
	}

	override fun onSupportNavigateUp(): Boolean {
		return findNavController(this, R.id.nav_host).navigateUp()
	}

	private fun setupNavigation() {
		setSupportActionBar(findViewById(R.id.toolbar))
		/*used this instead of findNavController because that one requires views to be laid out
		already, i.e. can be used after Activity.onCreate or inside fragment*/
		(supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment).let {
			NavigationUI.setupActionBarWithNavController(this, it.navController)
		}
	}
}