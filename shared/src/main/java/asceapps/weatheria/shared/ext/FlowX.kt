package asceapps.weatheria.shared.ext

import asceapps.weatheria.shared.data.repo.Error
import asceapps.weatheria.shared.data.repo.Loading
import asceapps.weatheria.shared.data.repo.Result
import asceapps.weatheria.shared.data.repo.Success
import kotlinx.coroutines.flow.*

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