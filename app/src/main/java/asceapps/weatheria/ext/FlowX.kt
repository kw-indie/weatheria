package asceapps.weatheria.ext

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

inline fun <T> Flow<T>.observe(lo: LifecycleOwner, crossinline block: suspend (T) -> Unit) =
	lo.lifecycleScope.launch {
		lo.repeatOnLifecycle(Lifecycle.State.STARTED) {
			collect(block)
		}
	}