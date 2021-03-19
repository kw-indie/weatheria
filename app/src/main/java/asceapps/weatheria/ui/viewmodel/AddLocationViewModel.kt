package asceapps.weatheria.ui.viewmodel

import android.annotation.SuppressLint
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import asceapps.weatheria.data.repo.Result
import asceapps.weatheria.data.repo.WeatherInfoRepo
import asceapps.weatheria.util.awaitCurrentLocation
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddLocationViewModel @Inject constructor(
	private val infoRepo: WeatherInfoRepo
) : ViewModel() {

	private val _myLocation = MutableStateFlow<Result<Location>>(Result.Loading)
	val myLocation: Flow<Result<Location>> get() = _myLocation
	private var query = MutableStateFlow("")
	val result = query
		.debounce(1000)
		.flatMapLatest { q -> infoRepo.search(q) }

	fun setQuery(q: String) {
		if (query.value != q)
			query.value = q
	}

	fun getMyLocation(client: FusedLocationProviderClient) = viewModelScope.launch {
		_myLocation.value = Result.Loading
		try {
			@SuppressLint("MissingPermission")
			_myLocation.value = Result.Success(client.awaitCurrentLocation())
		} catch (e: Exception) {
			e.printStackTrace()
			_myLocation.value = Result.Error(e)
		}
	}
}