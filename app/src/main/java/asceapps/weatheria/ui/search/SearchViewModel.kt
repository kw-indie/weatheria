package asceapps.weatheria.ui.search

import android.annotation.SuppressLint
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import asceapps.weatheria.data.LocationDao
import asceapps.weatheria.data.LocationEntity
import asceapps.weatheria.data.WeatherInfoRepo
import asceapps.weatheria.util.cleanCoordinates
import asceapps.weatheria.util.debounce
import asceapps.weatheria.util.getFreshLocation
import asceapps.weatheria.util.isCoordinate
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.launch

class SearchViewModel @ViewModelInject constructor(
	private val locationDao: LocationDao,
	private val repo: WeatherInfoRepo
): ViewModel() {

	val q = MutableLiveData<String>()
	val result = q
		.debounce(300, viewModelScope)
		.map {it.trim()}
		.distinctUntilChanged()
		.switchMap {q ->
			when {
				q.isEmpty() -> {
					liveData {emit(emptyList<LocationEntity>())}
				}
				isCoordinate(q) -> {
					val (lat, lng) = cleanCoordinates(q).split(',')
						.map {it.toFloat()}

					val radius = 1

					locationDao.find(
						lat, lng,
						lat - radius,
						lat + radius,
						lng - radius,
						lng + radius
					)
				}
				else -> {
					locationDao.find(q)
				}
			}
		}

	private val _error = MutableLiveData<Throwable>()
	val error: LiveData<Throwable> get() = _error

	fun addNewLocation(l: LocationEntity) {
		viewModelScope.launch {
			try {
				repo.fetch(l)
				// todo give positive feedback when successful
			} catch(e: Exception) {
				e.printStackTrace()
				_error.value = e
			}
		}
	}

	fun getCurrentLocation(client: FusedLocationProviderClient) {
		viewModelScope.launch {
			try {
				@SuppressLint("MissingPermission")
				val location = client.getFreshLocation()
				q.value = with(location) {"$latitude,$longitude"}
			} catch(e: Exception) {
				e.printStackTrace()
				_error.value = e
			}
		}
	}
}