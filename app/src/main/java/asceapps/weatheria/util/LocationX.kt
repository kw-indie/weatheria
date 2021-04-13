package asceapps.weatheria.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.location.LocationManagerCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val permissions = arrayOf(
	// coarse location alone is useless if we wanna guarantee a location update on fresh phones
	// if we are gonna guarantee a fresh location, might as well just use 1 strong permission for simplicity
	//Manifest.permission.ACCESS_COARSE_LOCATION,
	Manifest.permission.ACCESS_FINE_LOCATION
)
// needed to be HIGH for guaranteeing a location update on fresh devices
private const val defaultPriority = LocationRequest.PRIORITY_HIGH_ACCURACY

fun isLocationPermissionGranted(ctx: Context) = permissions.any {
	ActivityCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
}

fun shouldShowLocationPermissionRationale(activity: Activity) = permissions.any {
	ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
}

inline fun Fragment.createPermissionRequester(crossinline callback: (Boolean) -> Unit) =
	registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
		callback(it.values.any { granted -> granted }) // true if any  permission was granted
	}

fun requestLocationPermission(launcher: ActivityResultLauncher<Array<String>>) {
	launcher.launch(permissions)
}

@SuppressLint("MissingPermission")
suspend fun FusedLocationProviderClient.awaitCurrentLocation() = suspendCancellableCoroutine<Location?> {
	val cts = CancellationTokenSource()
	// Setting priority to BALANCED may only work on a real device that is also connected to
	// wifi, cellular, bluetooth, etc.
	// Anything lower will never get a fresh location when called on a device with no cached location.
	// Setting it to HIGH guarantees that if GPS is enabled (device only or high accuracy settings),
	// a fresh location will be fetched.
	getCurrentLocation(defaultPriority, cts.token)
		.addOnSuccessListener { location ->
			it.resume(location)
		}.addOnFailureListener { e ->
			it.resumeWithException(e)
		}

	it.invokeOnCancellation {
		cts.cancel()
	}
}

fun createLocationRequest(
	updates: Int = Int.MAX_VALUE,
	accuracy: Int = defaultPriority
): LocationRequest = LocationRequest.create().apply {
	isWaitForAccurateLocation = false
	numUpdates = updates
	priority = accuracy
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