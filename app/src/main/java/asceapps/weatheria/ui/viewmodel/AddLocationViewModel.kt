package asceapps.weatheria.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import asceapps.weatheria.data.repo.LocationRepo
import asceapps.weatheria.data.repo.SettingsRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddLocationViewModel @Inject constructor(
	private val locationRepo: LocationRepo,
	private val settingsRepo: SettingsRepo
): ViewModel() {

	val useDeviceForLocation: Boolean get() = settingsRepo.useDeviceForLocation
	val useHighAccuracyLocation: Boolean get() = settingsRepo.useHighAccuracyLocation
	val deviceLocation = locationRepo.deviceLocation
	val ipGeolocation = locationRepo.ipGeolocation

	private var query = MutableStateFlow("")
	val searchResult = query
		.debounce(1000)
		.flatMapLatest { q -> locationRepo.search(q) }

	fun setQuery(q: String) {
		query.value = q // already checked for 'same value' internally (in flow)
	}

	fun awaitDeviceLocation(accuracy: Int) = viewModelScope.launch {
		locationRepo.awaitDeviceLocation(accuracy)
	}

	fun awaitIpGeolocation() = viewModelScope.launch {
		locationRepo.awaitIpGeolocation()
	}
}