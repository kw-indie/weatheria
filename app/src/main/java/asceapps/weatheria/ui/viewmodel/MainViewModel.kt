package asceapps.weatheria.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import asceapps.weatheria.data.entity.LocationEntity
import asceapps.weatheria.data.entity.SavedLocationEntity
import asceapps.weatheria.data.repo.WeatherInfoRepo
import asceapps.weatheria.model.Location
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
	private val infoRepo: WeatherInfoRepo
): ViewModel() {

	val weatherInfoList = infoRepo.getAll()
		.map {
			_loading.value = false
			// fixme when fkers introduce onEach to liveData
			it
		}
	val savedLocationsList = infoRepo.getSavedLocations()
	val selectedLocation = MutableLiveData<SavedLocationEntity>(savedLocationsList.value?.get(0))
	val selectedLocationWeatherInfo = selectedLocation
		.map {
			_loading.value = true
			// fixme same as above
			it
		}
		.switchMap {infoRepo.get(it.id)}
		.map {
			_loading.value = false
			// fixme same as above
			it
		}
	private val _loading = MutableLiveData(true)
	val loading: LiveData<Boolean> get() = _loading
	private val _error = MutableLiveData<Throwable>()
	val error: LiveData<Throwable> = _error

	fun addNewLocation(l: LocationEntity) {
		_loading.value = true
		try {
			viewModelScope.launch {
				infoRepo.fetch(l)
			}
		} catch(e: Exception) {
			e.printStackTrace()
			_error.value = e
		} finally {
			_loading.value = false
		}
	}

	fun refresh(l: Location) {
		_loading.value = true
		try {
			viewModelScope.launch {
				with(l) {infoRepo.refresh(id, lat, lng)}
			}
		} catch(e: Exception) {
			e.printStackTrace()
			_error.value = e
		} finally {
			_loading.value = false
		}
	}

	fun reorder(l: Location, toPos: Int) {
		viewModelScope.launch {
			infoRepo.reorder(l.id, l.pos, toPos)
		}
	}

	fun delete(l: Location) {
		viewModelScope.launch {
			infoRepo.delete(l.id, l.pos)
		}
	}

	fun retain(l: Location) {
		viewModelScope.launch {
			infoRepo.retain(l.id)
		}
	}

	fun deleteAll() {
		viewModelScope.launch {
			infoRepo.deleteAll()
		}
	}
}