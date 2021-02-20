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
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException

@AndroidEntryPoint
class MainActivity: AppCompatActivity() {

	private val viewModel: MainViewModel by viewModels()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		WindowCompat.setDecorFitsSystemWindows(window, true)
		window.statusBarColor = 0

		setupNavigation()

		val snackbar = Snackbar.make(
			findViewById(R.id.nav_host),
			R.string.error_no_internet,
			Snackbar.LENGTH_INDEFINITE
		).apply {
			animationMode = Snackbar.ANIMATION_MODE_SLIDE
			setAction(R.string.retry) {
				viewModel.checkOnline()
			}
		}
		viewModel.onlineStatus.observe(this) {
			if(it) snackbar.dismiss()
			else snackbar.show()
		}

		viewModel.error.observe(this) {
			Toast.makeText(
				this,
				when(it) {
					// obvious timeout
					is TimeoutCancellationException -> R.string.error_timed_out
					is IOException -> R.string.error_no_internet
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