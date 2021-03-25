package asceapps.weatheria.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.annotation.RequiresPermission
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import asceapps.weatheria.data.repo.Result
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun SharedPreferences.onChangeFlow() = callbackFlow<String> {
	val changeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
		try {
			offer(key)
		} catch (e: Exception) {
			close(e)
		}
	}
	registerOnSharedPreferenceChangeListener(changeListener)

	awaitClose {
		unregisterOnSharedPreferenceChangeListener(changeListener)
	}
}

fun SearchView.onTextSubmitFlow() = callbackFlow<String> {
	setOnQueryTextListener(object : SearchView.OnQueryTextListener {
		override fun onQueryTextSubmit(query: String): Boolean {
			offer(query)
			clearFocus()
			return true
		}

		override fun onQueryTextChange(newText: String): Boolean {
			return true
		}
	})

	awaitClose {
		setOnQueryTextListener(null)
	}
}

fun RecyclerView.Adapter<*>.onItemInsertedFlow() = callbackFlow<Int> {
	val observer = object : RecyclerView.AdapterDataObserver() {
		override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
			if (itemCount == 1)
				offer(positionStart)
		}
	}
	registerAdapterDataObserver(observer)

	awaitClose {
		unregisterAdapterDataObserver(observer)
	}
}

fun ViewPager2.onPageSelectedFlow() = callbackFlow<Int> {
	val callback = object : ViewPager2.OnPageChangeCallback() {
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
fun Context.onlineStatusFlow() = onlineStatusFlow(
	getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
)

private fun onlineStatusFlow(cm: ConnectivityManager) = callbackFlow {
	val request = NetworkRequest.Builder()
		.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
		.build()

	val callback = object : ConnectivityManager.NetworkCallback() {
		override fun onAvailable(network: Network) {
			refresh()
		}

		override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
			refresh()
		}

		override fun onLost(network: Network) {
			//offer(error) may have lost one network, but not the others
			refresh()
		}

		private fun refresh() {
			offer(Result.Loading)
			offer(blockingPing())
		}
	}

	// fire initial value
	offer(Result.Loading)
	offer(asyncPing())

	cm.registerNetworkCallback(request, callback)

	awaitClose {
		cm.unregisterNetworkCallback(callback)
	}
}

// todo move to a repo with other methods and share a dispatcher
suspend fun asyncPing() = withContext(Dispatchers.IO) {
	blockingPing()
}

// todo throttle, don't start new if already started
private fun blockingPing(): Result<Unit> {
	return try {
		Socket().use {
			// dns servers listen on port 53
			val socketAddress = InetSocketAddress("8.8.8.8", 53)
			it.connect(socketAddress, 1500)
		}
		Result.Success(Unit)
	} catch (e: Exception) {
		e.printStackTrace()
		Result.Error(e)
	}
}

@SuppressLint("MissingPermission")
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
			.addOnSuccessListener { location ->
				it.resume(location)
			}.addOnFailureListener { e ->
				it.resumeWithException(e)
			}

		it.invokeOnCancellation {
			cts.cancel()
		}
	}

fun <T> LiveData<T>.debounce(timeoutMillis: Int, scope: CoroutineScope): LiveData<T> =
	MediatorLiveData<T>().also { mld ->
		var job: Job? = null
		mld.addSource(this) {
			job?.cancel()
			job = scope.launch {
				delay(timeoutMillis.toLong())
				mld.value = it
			}
		}
	}

inline fun <T> LiveData<T>.filter(crossinline predicate: (T) -> Boolean): LiveData<T> =
	MediatorLiveData<T>().also { mld ->
		mld.addSource(this) {
			if (predicate(it)) mld.value = it
		}
	}

inline fun <T> LiveData<T>.onEach(crossinline block: (T) -> Unit): LiveData<T> =
	MediatorLiveData<T>().also { mld ->
		mld.addSource(this) {
			block(it)
			mld.value = it
		}
	}

inline fun <T> Flow<T>.observe(owner: LifecycleOwner, crossinline block: suspend (T) -> Unit) =
	owner.addRepeatingJob(Lifecycle.State.STARTED) {
		collect(block)
	}