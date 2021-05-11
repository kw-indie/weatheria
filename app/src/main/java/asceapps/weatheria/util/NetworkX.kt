package asceapps.weatheria.util

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.annotation.RequiresPermission
import asceapps.weatheria.data.repo.Error
import asceapps.weatheria.data.repo.Loading
import asceapps.weatheria.data.repo.Result
import asceapps.weatheria.data.repo.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
fun Context.onlineStatusFlow() = onlineStatusFlow(
	getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
)

private const val TIMEOUT = 5000
private fun onlineStatusFlow(cm: ConnectivityManager) = callbackFlow {
	val request = NetworkRequest.Builder()
		.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
		.build()

	val callback = object: ConnectivityManager.NetworkCallback() {
		private var lastCheck = 0L // thread local, no need for sync
		override fun onAvailable(network: Network) = refresh()

		override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = refresh()

		// do NOT send(error), since we may have lost one network, but not the others
		// for some reason, the OS calls this many times on each event (at least 4 times)
		override fun onLost(network: Network) = refresh()

		private fun refresh() {
			val now = System.currentTimeMillis()
			if(now > lastCheck + TIMEOUT) {
				lastCheck = now
				trySend(Loading)
				// this is already on a background thread
				trySend(blockingPing())
			}
		}
	}

	// fire initial value
	trySend(Loading)
	trySend(asyncPing())

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
			it.connect(socketAddress, TIMEOUT)
		}
		Success(Unit)
	} catch(e: Exception) {
		e.printStackTrace()
		Error(e)
	}
}