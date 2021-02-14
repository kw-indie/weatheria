package asceapps.weatheria.ui.viewmodel

import android.annotation.SuppressLint
import android.location.Location
import androidx.annotation.RequiresPermission
import androidx.lifecycle.*
import asceapps.weatheria.data.repo.LocationRepo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@HiltViewModel
class SearchViewModel @Inject constructor(
	private val locationRepo: LocationRepo
): ViewModel() {

	// todo save query in saveStateHandler and build 'result' reacting to that live data
	// see https://github.com/android/architecture-components-samples/blob/master/BasicSample/app/src/main/java/com/example/android/persistence/viewmodel/ProductListViewModel.java
	private val _mylocation = MutableLiveData<Location>()
	val myLocation: LiveData<Location> = _mylocation
	val query = MutableLiveData<String>() // todo convert to stateFlow in AS 4.3
	val result = query
		.debounce(500, viewModelScope)
		.map {it.trim()}
		.distinctUntilChanged()
		.switchMap {q -> locationRepo.search(q)}
	/*.asFlow()
	.debounce(500)
	.filterNot {it.isNullOrBlank()}
	.distinctUntilChanged()
	.flatMapLatest {q -> locationRepo.search(q)}*/

	private val _error = MutableLiveData<Throwable>()
	val error: LiveData<Throwable> get() = _error

	fun getMyLocation(client: FusedLocationProviderClient) {
		viewModelScope.launch {
			try {
				@SuppressLint("MissingPermission")
				_mylocation.value = client.awaitCurrentLocation()
			} catch(e: Exception) {
				e.printStackTrace()
				_error.value = e
			}
		}
	}

	companion object {

		private fun <T> LiveData<T>.debounce(timeoutMillis: Long, scope: CoroutineScope) =
			MediatorLiveData<T>().also {mld ->
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
		private suspend fun FusedLocationProviderClient.awaitCurrentLocation() =
			suspendCancellableCoroutine<Location> {
				val cts = CancellationTokenSource()
				// Setting priority to BALANCED may only work on a real device that is also connected to
				// wifi, cellular, bluetooth, etc. anything lower will never get a fresh location
				// when called on a device with no cached location.
				// Having it at high guarantees that if GPS is enabled (device only, high accuracy settings),
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
	}
}