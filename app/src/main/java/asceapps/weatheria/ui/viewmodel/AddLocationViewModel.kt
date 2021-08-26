package asceapps.weatheria.ui.viewmodel

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import asceapps.weatheria.data.api.SearchResponse
import asceapps.weatheria.data.repo.SettingsRepo
import asceapps.weatheria.data.repo.WeatherInfoRepo
import asceapps.weatheria.shared.data.repo.Result
import asceapps.weatheria.util.awaitCurrentLocation
import asceapps.weatheria.util.resultFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddLocationViewModel @Inject constructor(
	private val infoRepo: WeatherInfoRepo,
	private val settingsRepo: SettingsRepo
): ViewModel() {

	val useDeviceForLocation get() = settingsRepo.useDeviceForLocation
	val useHighAccuracyLocation get() = settingsRepo.useHighAccuracyLocation

	private val query = MutableStateFlow("")
	private val ipSearch = MutableSharedFlow<Result<List<SearchResponse>>>(1)
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

	fun getDeviceLocation(ctx: Context, accuracy: Int) = resultFlow<Location> {
		ctx.awaitCurrentLocation(accuracy)
	}

	fun add(l: SearchResponse) = infoRepo.add(l)
}