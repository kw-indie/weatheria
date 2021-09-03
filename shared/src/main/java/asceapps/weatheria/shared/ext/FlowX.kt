package asceapps.weatheria.shared.ext

import asceapps.weatheria.shared.data.result.Error
import asceapps.weatheria.shared.data.result.Loading
import asceapps.weatheria.shared.data.result.Result
import asceapps.weatheria.shared.data.result.Success
import kotlinx.coroutines.flow.*

// fixme inlining a methods used this frequently prolly has a bad effect
inline fun <T> resultFlow(
	crossinline errorTransformer: (Throwable) -> Throwable = { it },
	crossinline block: suspend FlowCollector<Result<T>>.() -> T
) = flow {
	emit(Loading())
	try {
		emit(Success(block()))
	} catch(t: Throwable) {
		t.printStackTrace()
		emit(Error(errorTransformer(t)))
	}
}

fun <T> Flow<T>.asResult() = flow {
	emit(Loading())
	emitAll(map { Success(it) })
}