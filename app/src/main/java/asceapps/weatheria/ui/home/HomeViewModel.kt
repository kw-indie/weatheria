package asceapps.weatheria.ui.home

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import asceapps.weatheria.data.WeatherInfoRepo
import asceapps.weatheria.model.Location
import kotlinx.coroutines.launch

class HomeViewModel @ViewModelInject constructor(private val repo: WeatherInfoRepo): ViewModel() {

	val infoList = repo.getAll()
		.map {
			_refreshing.value = false
			it
		}
	private val _error = MutableLiveData<Throwable>()
	val error: LiveData<Throwable> get() = _error
	private val _refreshing = MutableLiveData(true)
	val refreshing: LiveData<Boolean> get() = _refreshing

	fun update(l: Location) {
		viewModelScope.launch {
			_refreshing.value = true
			try {
				with(l) {repo.refresh(id, lat, lng)}
			} catch(e: Exception) {
				e.printStackTrace()
				_error.value = e
			} finally {
				_refreshing.value = false
			}
		}
	}

	fun delete(l: Location) {
		viewModelScope.launch {
			repo.delete(l)
		}
	}

	fun retain(l: Location) {
		viewModelScope.launch {
			repo.retain(l)
		}
	}

	fun deleteAll() {
		viewModelScope.launch {
			repo.deleteAll()
		}
	}
}