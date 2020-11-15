package asceapps.weatheria.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import asceapps.weatheria.model.WeatherInfoRepo
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

private fun isCoordinate(str: String) = str.matches(Regex(
	"^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?)\\s*,\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)\$"
))

class HomeViewModel(private val repo: WeatherInfoRepo): ViewModel() {

	val infoList = repo.getAll()
		.onStart {_refreshing.value = true}
		.onEach {_refreshing.value = false}
		.asLiveData()
	private val _error = MutableLiveData<Throwable>()
	val error: LiveData<Throwable> get() = _error
	private val _refreshing = MutableLiveData<Boolean>()
	val refreshing: LiveData<Boolean> get() = _refreshing
	var selected: Int? = null

	/**
	 * If successful, the result is auto inserted in the database.
	 * @param query user input. a location name or comma separated coordinates
	 */
	fun addNewLocation(query: String) = viewModelScope.launch {
		_refreshing.value = true
		try {
			if(isCoordinate(query)) {
				// this 7 is for cases like -xx.yyy, we want max 6 but 7th for accuracy
				// need to clamp because gps gives way too many decimals
				val coords = query.split(',').map {it.take(7).trim()}
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

	fun deleteAll() = viewModelScope.launch {
		repo.deleteAll()
	}

	class Factory(
		private val weatherInfoRepo: WeatherInfoRepo
	): ViewModelProvider.Factory {

		@Suppress("unchecked_cast")
		override fun <T: ViewModel?> create(modelClass: Class<T>): T =
			HomeViewModel(weatherInfoRepo) as T
	}
}