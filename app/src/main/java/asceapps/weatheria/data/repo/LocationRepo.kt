package asceapps.weatheria.data.repo

import asceapps.weatheria.data.api.FindResponse
import asceapps.weatheria.data.api.IPApi
import asceapps.weatheria.data.api.WeatherApi
import asceapps.weatheria.di.IoDispatcher
import asceapps.weatheria.util.awaitCurrentLocation
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@ViewModelScoped
class LocationRepo @Inject constructor(
	private val locationProvider: FusedLocationProviderClient,
	private val ipApi: IPApi,
	private val weatherApi: WeatherApi,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

	fun getDeviceLocation(accuracy: Int) = flow {
		emit(Result.Loading)
		try {
			// important (!!) so that NPE is caught as Result.Error
			val location = locationProvider.awaitCurrentLocation(accuracy)!!
			emit(Result.Success(location))
		} catch(e: Exception) {
			e.printStackTrace()
			emit(Result.Error(e))
		}
	}

	fun getIpGeolocation() = flow {
		emit(Result.Loading)
		try {
			val latLng = ipApi.lookup()
			emit(Result.Success(latLng))
		} catch(e: Exception) {
			e.printStackTrace()
			emit(Result.Error(e))
		}
	}.flowOn(ioDispatcher)

	fun search(query: String) = flow {
		emit(Result.Loading)
		try {
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
		} catch(e: Exception) {
			e.printStackTrace()
			emit(Result.Error(e))
		}
	}.flowOn(ioDispatcher)

	companion object {

		private val coordinateRegex = Regex(
			"^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?)\\s*,\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)\$"
		)
	}
}