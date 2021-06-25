package asceapps.weatheria.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import asceapps.weatheria.data.repo.Error
import asceapps.weatheria.data.repo.Loading
import asceapps.weatheria.data.repo.Result
import asceapps.weatheria.data.repo.Success
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

inline fun <T> Flow<T>.observe(lo: LifecycleOwner, crossinline block: suspend (T) -> Unit) =
	lo.lifecycleScope.launch {
		lo.repeatOnLifecycle(Lifecycle.State.STARTED) {
			collect(block)
		}
	}

inline fun <T> resultFlow(crossinline block: suspend FlowCollector<Result<T>>.() -> T) = flow {
	emit(Loading())
	try {
		emit(Success(block()))
	} catch(e: Exception) {
		e.printStackTrace()
		emit(Error(e))
	}
}

fun <T> Flow<T>.asResult() = flow {
	emit(Loading())
	emitAll(map { Success(it) })
}