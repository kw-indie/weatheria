package asceapps.weatheria.ui.viewmodel

import androidx.lifecycle.ViewModel
import asceapps.weatheria.data.api.FindResponse
import asceapps.weatheria.data.repo.LocationRepo
import asceapps.weatheria.data.repo.SettingsRepo
import asceapps.weatheria.data.repo.WeatherInfoRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class AddLocationViewModel @Inject constructor(
	private val locationRepo: LocationRepo,
	private val infoRepo: WeatherInfoRepo,
	private val settingsRepo: SettingsRepo
): ViewModel() {

	val useDeviceForLocation: Boolean get() = settingsRepo.useDeviceForLocation
	val useHighAccuracyLocation: Boolean get() = settingsRepo.useHighAccuracyLocation

	private val query = MutableStateFlow("")
	val searchResult = query
		.debounce(1000)
		.flatMapLatest { q -> locationRepo.search(q) }

	fun search(q: String) {
		query.value = q // already checked for 'same value' internally (in flow)
	}

	fun getDeviceLocation(accuracy: Int) = locationRepo.getDeviceLocation(accuracy)

	fun getIpGeolocation() = locationRepo.getIpGeolocation()

	fun add(l: FindResponse.Location) = infoRepo.add(l)
}