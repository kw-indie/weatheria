package asceapps.weatheria.ui.home

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import asceapps.weatheria.data.WeatherInfoRepo
import asceapps.weatheria.util.isCoordinate
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class HomeViewModel @ViewModelInject constructor(private val repo: WeatherInfoRepo): ViewModel() {

	val infoList = repo.getAll()
		.onEach {_refreshing.value = false}
		.asLiveData()
	private val _error = MutableLiveData<Throwable>()
	val error: LiveData<Throwable> get() = _error
	private val _refreshing = MutableLiveData(true)
	val refreshing: LiveData<Boolean> get() = _refreshing
	var selected: Int? = null

	/**
	 * If successful, the result is auto inserted in the database.
	 * @param query user input. a location name or comma separated coordinates
	 */
	fun addNewLocation(query: String) {
		viewModelScope.launch {
			_refreshing.value = true
			try {
				if(isCoordinate(query)) {
					// this 7 is for cases like -xx.yyy, we want max 6 but 7th for accuracy
					// need to clamp because gps gives way too many decimals
					val coords = query.split(',').onEach {it.take(7).trim()}
					repo.get(coords[0], coords[1])
				} else {
					repo.get(query.trim())
				}
			} catch(e: Exception) {
				e.printStackTrace()
				_error.value = e
			} finally {
				_refreshing.value = false
			}
		}
	}

	fun updateSelected() {
		selected?.let {selected ->
			infoList.value?.get(selected)?.location
		}?.let {location ->
			viewModelScope.launch {
				_refreshing.value = true
				try {
					repo.getUpdate(location)
				} catch(e: Exception) {
					e.printStackTrace()
					_error.value = e
				} finally {
					_refreshing.value = false
				}
			}
		}
	}

	fun deleteSelected() {
		selected?.let {selected ->
			infoList.value?.get(selected)?.location
		}?.let {location ->
			viewModelScope.launch {
				repo.delete(location)
			}
		}
	}

	fun deleteAll() {
		viewModelScope.launch {
			repo.deleteAll()
		}
	}
}