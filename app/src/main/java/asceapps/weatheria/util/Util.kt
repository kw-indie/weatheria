package asceapps.weatheria.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun hideKeyboard(any: View) {
	ViewCompat.getWindowInsetsController(any)?.hide(WindowInsetsCompat.Type.ime())
}

fun isKeyboardVisible(any: View) =
	ViewCompat.getRootWindowInsets(any)?.isVisible(WindowInsetsCompat.Type.ime())

fun View.edgeToEdge(
	consume: Boolean = true,
	statusBar: Boolean = true,
	navBar: Boolean = true
) {
	ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
		val navBarInsets =
			if (navBar) insets.getInsets(WindowInsetsCompat.Type.navigationBars()) else null
		val statusBarInsets =
			if (statusBar) insets.getInsets(WindowInsetsCompat.Type.statusBars()) else null
		setPadding(
			navBarInsets?.left ?: 0,
			statusBarInsets?.top ?: 0,
			navBarInsets?.right ?: 0,
			navBarInsets?.bottom ?: 0
		)
		setOnApplyWindowInsetsListener(null)
		if (consume) WindowInsetsCompat.CONSUMED else insets
	}
}
