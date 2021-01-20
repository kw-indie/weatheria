package asceapps.weatheria.ui.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import asceapps.weatheria.data.entity.SavedLocationEntity
import asceapps.weatheria.data.repo.WeatherInfoRepo
import kotlinx.coroutines.launch

class LocationsViewModel @ViewModelInject constructor(
	private val repo: WeatherInfoRepo
): ViewModel() {

	fun reorder(l: SavedLocationEntity, toPos: Int) {
		viewModelScope.launch {
			repo.reorder(l, toPos)
		}
	}

	fun delete(l: SavedLocationEntity) {
		viewModelScope.launch {
			repo.delete(l)
		}
	}
}