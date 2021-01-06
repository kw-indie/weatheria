package asceapps.weatheria.ui.search

import android.annotation.SuppressLint
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import asceapps.weatheria.data.LocationEntity
import asceapps.weatheria.data.LocationRepo
import asceapps.weatheria.data.WeatherInfoRepo
import asceapps.weatheria.util.debounce
import asceapps.weatheria.util.getFreshLocation
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.launch

class SearchViewModel @ViewModelInject constructor(
	private val locationRepo: LocationRepo,
	private val infoRepo: WeatherInfoRepo
): ViewModel() {

	val savedLocations = infoRepo.getSavedLocations()
	val query = MutableLiveData<String>()
	val result = query
		.debounce(500, viewModelScope)
		.map {it.trim()}
		.distinctUntilChanged()
		.switchMap {q -> locationRepo.search(q)}

	private val _error = MutableLiveData<Throwable>()
	val error: LiveData<Throwable> get() = _error

	fun addNewLocation(l: LocationEntity) {
		viewModelScope.launch {
			try {
				infoRepo.fetch(l)
			} catch(e: Exception) {
				e.printStackTrace()
				_error.value = e
			}
		}
	}

	fun getMyLocation(client: FusedLocationProviderClient) {
		viewModelScope.launch {
			try {
				@SuppressLint("MissingPermission")
				val location = client.getFreshLocation()
				query.value = with(location) {"$latitude,$longitude"}
			} catch(e: Exception) {
				e.printStackTrace()
				_error.value = e
			}
		}
	}
}