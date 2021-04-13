package asceapps.weatheria.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.addRepeatingJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

inline fun <T> Flow<T>.observe(owner: LifecycleOwner, crossinline block: suspend (T) -> Unit) =
	owner.addRepeatingJob(Lifecycle.State.STARTED) {
		collect(block)
	}