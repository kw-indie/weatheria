package asceapps.weatheria.util

import android.content.res.Configuration
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

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

fun SearchView.onTextSubmitFlow() = callbackFlow {
	setOnQueryTextListener(object: SearchView.OnQueryTextListener {
		override fun onQueryTextSubmit(query: String): Boolean {
			trySend(query)
			return true
		}

		override fun onQueryTextChange(newText: String) = true
	})

	awaitClose {
		setOnQueryTextListener(null)
	}
}

fun RecyclerView.Adapter<*>.onItemInsertedFlow() = callbackFlow {
	val observer = object: RecyclerView.AdapterDataObserver() {
		override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
			if(itemCount == 1)
				trySend(positionStart)
		}
	}
	registerAdapterDataObserver(observer)

	awaitClose {
		unregisterAdapterDataObserver(observer)
	}
}

class PagerHelper(private val rv: RecyclerView): PagerSnapHelper() {

	private val lm = LinearLayoutManager(rv.context, LinearLayoutManager.HORIZONTAL, false)
	var isEnabled: Boolean = false
		set(value) {
			if(field != value) {
				field = value
				if(field) {
					attachToRecyclerView(rv)
					rv.layoutManager = lm
				} else {
					attachToRecyclerView(null)
					rv.layoutManager = null
				}
			}
		}

	init {
		isEnabled = true
	}

	var currentPage: Int
		get() {
			val lm = rv.layoutManager ?: return RecyclerView.NO_POSITION
			val snapView = findSnapView(lm) ?: return RecyclerView.NO_POSITION
			return lm.getPosition(snapView)
		}
		set(value) {
			setCurrentPage(value)
		}

	val pageSelectedFlow = callbackFlow {
		val callback = object: RecyclerView.OnScrollListener() {
			var snapPos = RecyclerView.NO_POSITION
			override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
				val snapPos = currentPage
				if(this.snapPos != snapPos) {
					this.snapPos = snapPos
					trySend(snapPos)
				}
			}
		}
		rv.addOnScrollListener(callback)

		awaitClose {
			rv.removeOnScrollListener(callback)
		}
	}

	fun setCurrentPage(pos: Int, smooth: Boolean = true) {
		if(smooth) rv.smoothScrollToPosition(pos) else rv.scrollToPosition(pos)
	}
}