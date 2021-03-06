package asceapps.weatheria.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import asceapps.weatheria.R
import asceapps.weatheria.databinding.ActivityMainBinding
import asceapps.weatheria.ui.viewmodel.MainViewModel
import asceapps.weatheria.util.edgeToEdge
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

	private val viewModel: MainViewModel by viewModels()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding =
			DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)

		WindowCompat.setDecorFitsSystemWindows(window, false)
		binding.navHost.edgeToEdge(statusBar = false)
		binding.appbar.edgeToEdge(navBarPortrait = false)

		setupNavigation(binding.toolbar)

		val snackbar =
			Snackbar.make(binding.navHost, R.string.error_no_internet, Snackbar.LENGTH_INDEFINITE)
				.apply {
					animationMode = Snackbar.ANIMATION_MODE_SLIDE
					setAction(R.string.retry) { viewModel.checkOnline() }
				}
		viewModel.onlineStatus.observe(this) {
			if (it) snackbar.dismiss()
			else snackbar.show()
		}

		viewModel.error.observe(this) {
			Toast.makeText(
				this,
				when (it) {
					// obvious timeout
					is InterruptedIOException -> R.string.error_timed_out
					// others like UnknownHostException when it can't resolve hostname
					is IOException -> R.string.error_no_internet
					// others like http error codes (400, 404, etc.)
					is HttpException -> R.string.error_server_error
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
		when (item.itemId) {
			R.id.action_settings -> findNavController(this, R.id.nav_host)
				.navigate(R.id.action_open_settings)
			else -> return super.onOptionsItemSelected(item)
		}
		return true
	}

	override fun onSupportNavigateUp(): Boolean {
		return findNavController(this, R.id.nav_host).navigateUp()
	}

	private fun setupNavigation(toolbar: Toolbar) {
		setSupportActionBar(toolbar)
		/*used this instead of findNavController because that one requires views to be laid out
		already, i.e. can be used after Activity.onCreate or inside fragment*/
		(supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment).let {
			NavigationUI.setupActionBarWithNavController(this, it.navController)
		}
	}
}