package asceapps.weatheria.util

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.annotation.RequiresPermission
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun SharedPreferences.onChangeFlow() = callbackFlow<String> {
	val changeListener = SharedPreferences.OnSharedPreferenceChangeListener {_, key ->
		try {
			offer(key)
		} catch(e: Exception) {
			close(e)
		}
	}
	registerOnSharedPreferenceChangeListener(changeListener)

	awaitClose {
		unregisterOnSharedPreferenceChangeListener(changeListener)
	}
}

fun RecyclerView.Adapter<*>.onItemInsertedFlow() = callbackFlow<Int> {
	val observer = object: RecyclerView.AdapterDataObserver() {
		override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
			if(itemCount == 1)
				offer(positionStart)
		}
	}
	registerAdapterDataObserver(observer)

	awaitClose {
		unregisterAdapterDataObserver(observer)
	}
}

fun ViewPager2.onPageSelectedFlow() = callbackFlow<Int> {
	val callback = object: ViewPager2.OnPageChangeCallback() {
		override fun onPageSelected(position: Int) {
			offer(position)
		}
	}
	registerOnPageChangeCallback(callback)

	awaitClose {
		unregisterOnPageChangeCallback(callback)
	}
}

@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
fun Context.onlineStatusFlow(): Flow<Boolean> = callbackFlow {
	val request = NetworkRequest.Builder()
		.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
		.build()

	val callback = object: ConnectivityManager.NetworkCallback() {
		override fun onAvailable(network: Network) {
			offer(blockingPing())
		}

		override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
			offer(blockingPing())
		}

		override fun onLost(network: Network) {
			//offer(false) may have lost one network, but not the others
			offer(blockingPing())
		}
	}

	// fire initial value
	offer(ping())

	val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
	cm.registerNetworkCallback(request, callback)

	awaitClose {
		cm.unregisterNetworkCallback(callback)
	}
}

suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
	blockingPing()
}

private fun blockingPing(): Boolean {
	return try {
		Socket().use {
			// dns servers listen on port 53
			val socketAddress = InetSocketAddress("8.8.8.8", 53)
			it.connect(socketAddress, 1500)
		}
		true
	} catch(e: Exception) {
		e.printStackTrace()
		false
	}
}

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

@RequiresPermission(value = Manifest.permission.ACCESS_COARSE_LOCATION)
suspend fun FusedLocationProviderClient.awaitCurrentLocation() =
	suspendCancellableCoroutine<Location> {
		val cts = CancellationTokenSource()
		// Setting priority to BALANCED may only work on a real device that is also connected to
		// wifi, cellular, bluetooth, etc. anything lower will never get a fresh location
		// when called on a device with no cached location.
		// Having it at high guarantees that if GPS is enabled (device only or high accuracy settings),
		// a fresh location will be fetched.
		// Our permission does not force enabling GPS, thus, a device can just give a cached location.
		getCurrentLocation(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
			.addOnSuccessListener {location ->
				it.resume(location)
			}.addOnFailureListener {e ->
				it.resumeWithException(e)
			}

		it.invokeOnCancellation {
			cts.cancel()
		}
	}

fun SearchView.onTextChangeFlow() = callbackFlow<String> {
	setOnQueryTextListener(object: SearchView.OnQueryTextListener {
		override fun onQueryTextSubmit(query: String?): Boolean {
			clearFocus()
			return true
		}

		override fun onQueryTextChange(newText: String): Boolean {
			offer(newText)
			return true
		}
	})

	awaitClose {
		setOnQueryTextListener(null)
	}
}