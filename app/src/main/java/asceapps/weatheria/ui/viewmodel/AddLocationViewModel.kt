package asceapps.weatheria.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import asceapps.weatheria.data.repo.SettingsRepo
import asceapps.weatheria.ext.awaitCurrentLocation
import asceapps.weatheria.shared.data.model.Location
import asceapps.weatheria.shared.data.repo.WeatherInfoRepo
import asceapps.weatheria.shared.data.result.Result
import asceapps.weatheria.shared.ext.resultFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

typealias AndroidLocation = android.location.Location

@HiltViewModel
class AddLocationViewModel @Inject constructor(
	private val infoRepo: WeatherInfoRepo,
	settingsRepo: SettingsRepo
): ViewModel() {

	// these 2 can just be fields since the vm lives in 1 fragment and recreates with it
	val useDeviceForLocation = settingsRepo.useDeviceForLocation
	val useHighAccuracyLocation = settingsRepo.useHighAccuracyLocation

	private val query = MutableStateFlow("")
	private val ipSearch = MutableSharedFlow<Result<List<Location>>>(1)
	val searchResult = merge(
		query.debounce(1500)
			.filter { it.isNotBlank() }
			.flatMapLatest { q -> infoRepo.search(q) },
		ipSearch
	)

	fun search(q: String) {
		// already checked for 'same value' internally (in flow)
		query.value = q
	}

	fun searchByIP() = viewModelScope.launch {
		ipSearch.emitAll(infoRepo.search())
	}

	fun getDeviceLocation(ctx: Context, accuracy: Int) = resultFlow<AndroidLocation> {
		ctx.awaitCurrentLocation(accuracy)
	}

	fun add(l: Location) = infoRepo.add(l)
}