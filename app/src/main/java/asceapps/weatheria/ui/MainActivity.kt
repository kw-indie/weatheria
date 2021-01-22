package asceapps.weatheria.ui

import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
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
					is SQLiteConstraintException -> R.string.error_duplicate_location
					else -> R.string.error_unknown
				},
				Toast.LENGTH_LONG
			).show()
		}
	}
}