package asceapps.weatheria.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import asceapps.weatheria.data.model.FoundLocation
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
		// already checked for 'same value' internally (in flow)
		query.value = q
	}

	fun getDeviceLocation(ctx: Context, accuracy: Int) = locationRepo.getDeviceLocation(ctx, accuracy)

	fun getIpGeolocation() = locationRepo.getIpGeolocation()

	fun add(fl: FoundLocation) = infoRepo.add(fl)
}