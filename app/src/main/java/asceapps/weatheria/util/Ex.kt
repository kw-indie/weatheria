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

@RequiresPermission(value = "android.permission.ACCESS_COARSE_LOCATION")
suspend fun FusedLocationProviderClient.awaitCurrentLocation() =
	suspendCancellableCoroutine<Location> {
		val cts = CancellationTokenSource()
		// Setting priority to BALANCED may only work on a real device that is also connected to
		// wifi, cellular, bluetooth, etc. anything lower will never get a fresh location
		// when called on a device with no cached location.
		// Having it at high guarantees that if GPS is enabled (device only, high accuracy settings),
		// a fresh location will be fetched.
		// Our permission does not force enabling GPS, thus, a device can just give a cached location.
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