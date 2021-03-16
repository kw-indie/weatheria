package asceapps.weatheria.ui.viewmodel

import android.annotation.SuppressLint
import android.location.Location
import androidx.lifecycle.*
import asceapps.weatheria.data.repo.LocationRepo
import asceapps.weatheria.data.repo.Result
import asceapps.weatheria.util.awaitCurrentLocation
import asceapps.weatheria.util.debounce
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddLocationViewModel @Inject constructor(
	private val locationRepo: LocationRepo
) : ViewModel() {

	private val _myLocation = MutableLiveData<Result<Location>>()
	val myLocation: LiveData<Result<Location>> get() = _myLocation
	private var query = MutableLiveData<String>()
	val result = query
		.debounce(500, viewModelScope)
		//.filter { it.isNotBlank() } // this is actually not good since we want an empty list if we clear query
		.distinctUntilChanged()
		.switchMap { q -> locationRepo.search(q, viewModelScope.coroutineContext + Dispatchers.IO) }

	fun setQuery(q: String) {
		query.value = q
	}

	fun getMyLocation(client: FusedLocationProviderClient) = viewModelScope.launch {
		_myLocation.value = Result.Loading
		try {
			@SuppressLint("MissingPermission")
			_myLocation.value = Result.Success(client.awaitCurrentLocation())
		} catch (e: Exception) {
			e.printStackTrace()
			_myLocation.value = Result.Error(e)
		}
	}
}