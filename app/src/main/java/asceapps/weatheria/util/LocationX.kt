package asceapps.weatheria.util

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@SuppressLint("MissingPermission")
suspend fun FusedLocationProviderClient.awaitCurrentLocation(accuracy: Int) =
	suspendCancellableCoroutine<Location?> {
		val cts = CancellationTokenSource()
		// Setting priority to BALANCED may only work on a real device that is also connected to
		// wifi, cellular, bluetooth, etc.
		// Anything lower will never get a fresh location when called on a device with no cached location.
		// Setting it to HIGH guarantees that if GPS is enabled (device only or high accuracy settings),
		// a fresh location will be fetched.
		getCurrentLocation(accuracy, cts.token)
			.addOnSuccessListener { location ->
				it.resume(location)
			}.addOnFailureListener { e ->
				it.resumeWithException(e)
			}

		it.invokeOnCancellation {
			cts.cancel()
		}
	}

fun createLocationRequest(accuracy: Int, updates: Int = 1): LocationRequest =
	LocationRequest.create().apply {
		isWaitForAccurateLocation = false
		priority = accuracy
		numUpdates = updates
		interval = 1000 * 20
		fastestInterval = 1000 * 10
		smallestDisplacement = 5f
		maxWaitTime = 1000 * 60 * 1
		setExpirationDuration(1000 * 60 * 1)
	}

@SuppressWarnings("MissingPermission")
fun FusedLocationProviderClient.locationUpdates(request: LocationRequest) = callbackFlow<Location> {
	val callback = object: LocationCallback() {
		override fun onLocationResult(result: LocationResult?) {
			result ?: return
			for(location in result.locations) {
				try {
					offer(location)
				} catch(e: Exception) {
					close(e)
				}
			}
		}
	}

	requestLocationUpdates(request, callback, Looper.getMainLooper())
		.addOnFailureListener { e ->
			close(e)
		}

	awaitClose {
		removeLocationUpdates(callback)
	}
}

/**
 * Listens for changes to location mode on the device.
 * Emits false if the device has location off, true otherwise.
 * Current possible modes are Off, Sensor only, Battery saving, High accuracy.
 * Note that this is not the same as location provider changes.
 * @see [locationProviderChanges]
 */
fun Context.locationAvailabilityChanges() = callbackFlow<Boolean> {
	val receiver = object: BroadcastReceiver() {
		fun emitChange(context: Context) {
			val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
			val enabled = LocationManagerCompat.isLocationEnabled(lm)
			try {
				offer(enabled)
			} catch(e: Exception) {
				close(e)
			}
		}

		override fun onReceive(context: Context, intent: Intent) {
			emitChange(context)
		}
	}
	// fire initial value, then register for changes
	receiver.emitChange(this@locationAvailabilityChanges)

	val filter = IntentFilter(LocationManager.MODE_CHANGED_ACTION)
	registerReceiver(receiver, filter)

	awaitClose {
		unregisterReceiver(receiver)
	}
}

class LocationProviderChange(val isGpsEnabled: Boolean, val isNetworkEnabled: Boolean)

fun Context.locationProviderChanges() = callbackFlow<LocationProviderChange> {
	val receiver = object: BroadcastReceiver() {
		fun emitChange(context: Context) {
			val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
			val change = LocationProviderChange(
				lm.isProviderEnabled(LocationManager.GPS_PROVIDER),
				lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
			)
			try {
				offer(change)
			} catch(e: Exception) {
				close(e)
			}
		}

		override fun onReceive(context: Context, intent: Intent) {
			emitChange(context)
		}
	}
	// fire initial value, then register for changes
	receiver.emitChange(this@locationProviderChanges)

	val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
	registerReceiver(receiver, filter)

	awaitClose {
		unregisterReceiver(receiver)
	}
}