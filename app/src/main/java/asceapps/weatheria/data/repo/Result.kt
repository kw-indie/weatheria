package asceapps.weatheria.data.repo

sealed class Result<out T> {

	override fun toString(): String {
		return when(this) {
			is Loading -> "Loading"
			is Success -> "Success[data=$data]"
			is Error -> "Error[t=$t]"
		}
	}
}

object Loading: Result<Nothing>()
class Success<out T>(val data: T): Result<T>()
class Error(val t: Throwable): Result<Nothing>()