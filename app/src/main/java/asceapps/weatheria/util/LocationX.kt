package asceapps.weatheria.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@SuppressLint("MissingPermission")
suspend fun Context.awaitCurrentLocation(accuracy: Int) = suspendCancellableCoroutine<Location> {
	val cts = CancellationTokenSource()
	// Setting priority to BALANCED may only work on a real device that is also connected to
	// wifi, cellular, bluetooth, etc.
	// Anything lower will never get a fresh location when called on a device with no cached location.
	// Setting it to HIGH guarantees that if GPS is enabled (device only or high accuracy settings),
	// a fresh location will be fetched.
	LocationServices.getFusedLocationProviderClient(this).getCurrentLocation(accuracy, cts.token)
		.addOnSuccessListener { location ->
			try {
				it.resume(location!!)
			} catch(e: Exception) { // mainly for NPE
				it.resumeWithException(e)
			}
		}.addOnFailureListener { e ->
			// for others, such as SecurityException when insufficient permissions
			it.resumeWithException(e)
		}

	it.invokeOnCancellation {
		cts.cancel()
	}
}

/**
 * @param provider one of [LocationManager.GPS_PROVIDER] or [LocationManager.NETWORK_PROVIDER]
 */
@SuppressLint("MissingPermission")
suspend fun Context.awaitCurrentLocation(provider: String) = suspendCancellableCoroutine<Location> {
	val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
	val cs = CancellationSignal()
	val executor = Dispatchers.Default.asExecutor()
	LocationManagerCompat.getCurrentLocation(lm, provider, cs, executor) { location ->
		try {
			it.resume(location!!)
		} catch(e: Exception) {
			it.resumeWithException(e)
		}
	}

	it.invokeOnCancellation {
		cs.cancel()
	}
}

private fun createLocationRequest(accuracy: Int, updates: Int = 1): LocationRequest =
	LocationRequest.create().apply {
		isWaitForAccurateLocation = false
		priority = accuracy
		numUpdates = updates
		interval = 1000 * 20
		fastestInterval = 1000 * 10
		smallestDisplacement = 5f
		maxWaitTime = 1000 * 30 * 1
		setExpirationDuration(1000 * 30 * 1)
	}

sealed class LocationSettingsStatus {
	object Ready: LocationSettingsStatus()
	class Resolvable(val e: ResolvableApiException): LocationSettingsStatus()
	object Unavailable: LocationSettingsStatus()
}

// this works, but always requires Google Location Accuracy enabled
// which is not necessary for most applications
suspend fun checkLocationSettings(activity: Activity, accuracy: Int) =
	suspendCancellableCoroutine<LocationSettingsStatus> {
		val settingsRequest = LocationSettingsRequest.Builder()
			.addLocationRequest(createLocationRequest(accuracy))
			.build()
		LocationServices.getSettingsClient(activity)
			.checkLocationSettings(settingsRequest)
			.addOnSuccessListener { _ ->
				it.resume(LocationSettingsStatus.Ready)
			}
			.addOnFailureListener { e ->
				if(e is ResolvableApiException) {
					// Location settings are not satisfied, but could be fixed by showing a dialog.
					it.resume(LocationSettingsStatus.Resolvable(e))
					// resolve with:
					//val resolver = fragment.registerForActivityResult(
					//	ActivityResultContracts.StartIntentSenderForResult()
					//) {
					//	if(it.resultCode == Activity.RESULT_OK) {
					//		// resolved
					//	}
					//}
					//resolver.launch(IntentSenderRequest.Builder(e.resolution).build())
				} else { // LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE
					// Location settings are not satisfied. However, we have no way to fix the
					// settings so we won't show the dialog.
					it.resume(LocationSettingsStatus.Unavailable)
				}
			}

		// this request is prolly instant, it does not have a cancellation mechanism
		// dunno what to call in it.invokeOnCancellation {}
	}

@SuppressWarnings("MissingPermission")
fun FusedLocationProviderClient.locationUpdates(request: LocationRequest) = callbackFlow<Location> {
	val callback = object: LocationCallback() {
		override fun onLocationResult(result: LocationResult?) {
			if(result == null) return
			trySend(result.lastLocation) // i think if this fails, it auto closes channel
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
fun Context.locationAvailabilityChanges(lm: LocationManager) = callbackFlow<Boolean> {
	val receiver = object: BroadcastReceiver() {
		fun emitChange() {
			val enabled = LocationManagerCompat.isLocationEnabled(lm)
			trySend(enabled)
		}

		override fun onReceive(context: Context, intent: Intent) {
			emitChange()
		}
	}
	// fire initial value, then register for changes
	receiver.emitChange()

	val filter = IntentFilter(LocationManager.MODE_CHANGED_ACTION)
	registerReceiver(receiver, filter)

	awaitClose {
		unregisterReceiver(receiver)
	}
}

class LocationProviderChange(val isGpsEnabled: Boolean, val isNetworkEnabled: Boolean)

fun Context.locationProviderChanges(lm: LocationManager) = callbackFlow<LocationProviderChange> {
	val receiver = object: BroadcastReceiver() {
		fun emitChange() {
			val change = LocationProviderChange(
				lm.isProviderEnabled(LocationManager.GPS_PROVIDER),
				lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
			)
			trySend(change)
		}

		override fun onReceive(context: Context, intent: Intent) {
			emitChange()
		}
	}
	// fire initial value, then register for changes
	receiver.emitChange()

	val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
	registerReceiver(receiver, filter)

	awaitClose {
		unregisterReceiver(receiver)
	}
}