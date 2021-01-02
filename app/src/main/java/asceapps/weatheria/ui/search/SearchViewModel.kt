package asceapps.weatheria.ui.search

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import asceapps.weatheria.data.LocationDao
import asceapps.weatheria.data.LocationEntity
import asceapps.weatheria.data.WeatherInfoRepo
import asceapps.weatheria.util.isCoordinate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SearchViewModel @ViewModelInject constructor(
	private val locationDao: LocationDao,
	private val repo: WeatherInfoRepo
): ViewModel() {

	val query = MutableStateFlow("")
	val searchResult = query
		.debounce(300)
		.map {it.trim()}
		.distinctUntilChanged()
		.flatMapLatest {query ->
			when {
				query.isEmpty() -> {
					flowOf(emptyList())
				}
				isCoordinate(query) -> {
					val (lat, lng) = query.split(',')
						// this 7 is for cases like -xx.yyy, we want max 6 but 7th for accuracy
						// need to clamp because gps gives way too many decimals
						.map {it.trim().take(7).toFloat()}

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
					locationDao.find(query)
				}
			}
		}
		.asLiveData()
	private val _error = MutableLiveData<Throwable>()
	val error: LiveData<Throwable> get() = _error

	fun addNewLocation(l: LocationEntity) {
		viewModelScope.launch {
			try {
				repo.fetch(l)
				// todo give positive feedback when successful
			} catch(e: Throwable) {
				e.printStackTrace()
				_error.value = e
			}
		}
	}
}