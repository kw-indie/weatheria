package asceapps.weatheria.util

import android.content.res.Configuration
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

fun hideKeyboard(any: View) {
	ViewCompat.getWindowInsetsController(any)?.hide(WindowInsetsCompat.Type.ime())
}

fun isKeyboardVisible(any: View) =
	ViewCompat.getRootWindowInsets(any)?.isVisible(WindowInsetsCompat.Type.ime())

fun View.edgeToEdge(
	consume: Boolean = true,
	statusBar: Boolean = true,
	navBarPortrait: Boolean = true
) {
	ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
		val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
		val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
		val left: Int
		val top = if(statusBar) statusBarInsets.top else 0
		val right: Int
		val bot: Int
		val orientation = context.resources.configuration.orientation
		if(orientation == Configuration.ORIENTATION_PORTRAIT) {
			left = 0
			right = 0
			bot = if(navBarPortrait) navBarInsets.bottom else 0
		} else {
			// in landscape mode, (most prolly) all edgeToEdge views are affected by navBar insets
			left = navBarInsets.left // when navBar is on the left
			right = navBarInsets.right // when navBar is on the right
			bot = 0
		}
		setPadding(left, top, right, bot)
		// removing this listener here will work when launching the app for the first time,
		// but will fail when rotating while the app is still in the foreground
		if(consume) WindowInsetsCompat.CONSUMED else insets
	}
}

inline fun SearchView.onSubmit(crossinline block: (query: String) -> Unit) {
	setOnQueryTextListener(object: SearchView.OnQueryTextListener {
		override fun onQueryTextSubmit(query: String): Boolean {
			block(query)
			return true
		}

		override fun onQueryTextChange(newText: String) = true
	})
}

inline fun RecyclerView.Adapter<*>.onItemInserted(crossinline block: (atPos: Int) -> Unit) {
	val callback = object: RecyclerView.AdapterDataObserver() {
		override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
			if(itemCount == 1)
				block(positionStart)
		}
	}
	registerAdapterDataObserver(callback)
}

inline fun ViewPager2.onPageChanged(crossinline block: (pos: Int) -> Unit) {
	val callback = object: ViewPager2.OnPageChangeCallback() {
		override fun onPageSelected(position: Int) {
			block(position)
		}
	}
	registerOnPageChangeCallback(callback)
}