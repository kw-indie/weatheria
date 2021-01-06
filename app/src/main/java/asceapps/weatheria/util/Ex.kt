package asceapps.weatheria.util

import android.location.Location
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Instant
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun Int.toInstant(): Instant = Instant.ofEpochSecond(this.toLong())

fun <T> LiveData<T>.debounce(timeoutMillis: Long, scope: CoroutineScope) = MediatorLiveData<T>().also {mld ->
	var job: Job? = null
	mld.addSource(this) {
		job?.cancel()
		job = scope.launch {
			delay(timeoutMillis)
			mld.value = value
		}
	}
}

@RequiresPermission(anyOf = [
	"android.permission.ACCESS_COARSE_LOCATION",
	"android.permission.ACCESS_FINE_LOCATION"
])
suspend fun FusedLocationProviderClient.getFreshLocation() =
	suspendCancellableCoroutine<Location> {
		val cts = CancellationTokenSource()
		getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, cts.token)
			.addOnSuccessListener {location ->
				it.resume(location)
			}.addOnFailureListener {e ->
				it.resumeWithException(e)
			}

		it.invokeOnCancellation {
			cts.cancel()
		}
	}