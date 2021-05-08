package asceapps.weatheria.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.addRepeatingJob
import asceapps.weatheria.data.repo.Error
import asceapps.weatheria.data.repo.Loading
import asceapps.weatheria.data.repo.Success
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

inline fun <T> Flow<T>.observe(lo: LifecycleOwner, crossinline block: suspend (T) -> Unit) =
	lo.addRepeatingJob(Lifecycle.State.STARTED) {
		collect(block)
	}

inline fun <T> resultFlow(crossinline block: suspend () -> T) = flow {
	emit(Loading)
	try {
		emit(Success(block()))
	} catch(e: Exception) {
		e.printStackTrace()
		emit(Error(e))
	}
}

fun <T> Flow<T>.asResultFlow() = flow {
	emit(Loading)
	emitAll(map { Success(it) })
}