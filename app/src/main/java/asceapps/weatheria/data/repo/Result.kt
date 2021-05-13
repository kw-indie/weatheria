package asceapps.weatheria.data.repo

sealed class Result<out T> {

	override fun toString(): String {
		return when(this) {
			is Loading -> "Loading[%=$percent]"
			is Success -> "Success[data=$data]"
			is Error -> "Error[t=$t]"
		}
	}
}

class Loading(val percent: Int = -1): Result<Nothing>()
class Success<T>(val data: T): Result<T>()
class Error(val t: Throwable): Result<Nothing>()