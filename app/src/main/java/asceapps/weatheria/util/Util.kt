package asceapps.weatheria.util

import android.content.res.Configuration
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun hideKeyboard(any: View) {
	ViewCompat.getWindowInsetsController(any)?.hide(WindowInsetsCompat.Type.ime())
}

fun isKeyboardVisible(any: View) =
	ViewCompat.getRootWindowInsets(any)?.isVisible(WindowInsetsCompat.Type.ime())

fun View.edgeToEdge() {
	ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
		val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
		val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
		val config = view.context.resources.configuration
		when (config.orientation) {
			Configuration.ORIENTATION_PORTRAIT -> {
				setPadding(0, statusBarInsets.top, 0, navBarInsets.bottom)
			}
			else -> {
				setPadding(0, statusBarInsets.bottom, navBarInsets.right, 0)
			}
		}
		setOnApplyWindowInsetsListener(null)
		insets
	}
}
