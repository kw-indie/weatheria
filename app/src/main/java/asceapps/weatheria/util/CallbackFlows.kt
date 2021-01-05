package asceapps.weatheria.util

import android.location.Location
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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