package asceapps.weatheria.data.repo

import android.content.Context
import asceapps.weatheria.data.api.FindResponse
import asceapps.weatheria.data.api.IPApi
import asceapps.weatheria.data.api.WeatherApi
import asceapps.weatheria.di.IoDispatcher
import asceapps.weatheria.model.FoundLocation
import asceapps.weatheria.util.awaitCurrentLocation
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@ViewModelScoped
class LocationRepo @Inject constructor(
	private val ipApi: IPApi,
	private val weatherApi: WeatherApi,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

	fun getDeviceLocation(ctx: Context, accuracy: Int) = flow {
		emit(Loading)
		try {
			val location = ctx.awaitCurrentLocation(accuracy)
			emit(Success(location))
		} catch(e: Exception) {
			e.printStackTrace()
			emit(Error(e))
		}
	}

	fun getIpGeolocation() = flow {
		emit(Loading)
		try {
			val latLng = ipApi.lookup()
			emit(Success(latLng))
		} catch(e: Exception) {
			e.printStackTrace()
			emit(Error(e))
		}
	}.flowOn(ioDispatcher)

	fun search(query: String) = flow {
		emit(Loading)
		try {
			when {
				query.isEmpty() -> emit(Success(emptyList<FoundLocation>()))
				query.matches(coordinateRegex) -> {
					val (lat, lng) = query.split(',')
					val resp = weatherApi.find(lat, lng)
					val list = responseToModelList(resp)
					emit(Success(list))
				}
				else -> {
					val resp = weatherApi.find(query)
					val list = responseToModelList(resp)
					emit(Success(list))
				}
			}
		} catch(e: Exception) {
			e.printStackTrace()
			emit(Error(e))
		}
	}.flowOn(ioDispatcher)

	companion object {

		private val coordinateRegex = Regex(
			"^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?)\\s*,\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)\$"
		)

		private fun responseToModelList(fr: FindResponse) = fr.list.map {
			with(it) {
				FoundLocation(
					id,
					coord.lat,
					coord.lon,
					name,
					sys.country,
					main.temp,
					main.feels_like,
					main.pressure,
					main.humidity,
					wind.speed,
					wind.deg
				)
			}
		}
	}
}