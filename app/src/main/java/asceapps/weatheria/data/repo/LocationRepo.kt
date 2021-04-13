package asceapps.weatheria.data.repo

import android.location.Location
import asceapps.weatheria.data.api.FindResponse
import asceapps.weatheria.data.api.IPApi
import asceapps.weatheria.data.api.WeatherApi
import asceapps.weatheria.di.IoDispatcher
import asceapps.weatheria.util.awaitCurrentLocation
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ActivityRetainedScoped
class LocationRepo @Inject constructor(
	private val locationProvider: FusedLocationProviderClient,
	private val ipApi: IPApi,
	private val weatherApi: WeatherApi,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

	private val _deviceLocation = MutableStateFlow<Result<Location>>(Result.Init)
	val deviceLocation: Flow<Result<Location>> get() = _deviceLocation
	private val _ipGeolocation = MutableStateFlow<Result<String>>(Result.Init)
	val ipGeolocation: Flow<Result<String>> get() = _ipGeolocation

	suspend fun updateDeviceLocation() {
		_deviceLocation.value = Result.Loading
		try {
			withContext(ioDispatcher) {
				// important (!!) so that our logic catches all anomalies as exceptions, hence error results
				val location = locationProvider.awaitCurrentLocation()!!
				//val location = locationProvider.locationUpdates(createLocationRequest(1)).first()
				_deviceLocation.value = Result.Success(location)
			}
		} catch (e: Exception) {
			e.printStackTrace()
			_deviceLocation.value = Result.Error(e)
		}
	}

	suspend fun updateIpGeolocation() {
		_ipGeolocation.value = Result.Loading
		try {
			withContext(ioDispatcher) {
				val latLng = ipApi.lookup()
				_ipGeolocation.value = Result.Success(latLng)
			}
		} catch (e: Exception) {
			e.printStackTrace()
			_ipGeolocation.value = Result.Error(e)
		}
	}

	fun search(query: String) = flow {
		emit(Result.Loading)
		// todo catch errors
		when {
			query.isEmpty() -> emit(Result.Success(emptyList<FindResponse.Location>()))
			query.matches(coordinateRegex) -> {
				val (lat, lng) = query.split(',')
				val list = weatherApi.find(lat, lng).list
				emit(Result.Success(list))
			}
			else -> {
				val list = weatherApi.find(query).list
				emit(Result.Success(list))
			}
		}
	}.flowOn(ioDispatcher)

	companion object {

		private val coordinateRegex =
			Regex("^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?)\\s*,\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)\$")
	}
}