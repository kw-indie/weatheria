package asceapps.weatheria.data.repo

import android.content.Context
import asceapps.weatheria.data.api.FindResponse
import asceapps.weatheria.data.api.IPApi
import asceapps.weatheria.data.api.WeatherApi
import asceapps.weatheria.data.model.FoundLocation
import asceapps.weatheria.di.IoDispatcher
import asceapps.weatheria.util.awaitCurrentLocation
import asceapps.weatheria.util.resultFlow
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@ViewModelScoped
class LocationRepo @Inject constructor(
	private val ipApi: IPApi,
	private val weatherApi: WeatherApi,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

	fun getDeviceLocation(ctx: Context, accuracy: Int) = resultFlow {
		ctx.awaitCurrentLocation(accuracy)
	}

	fun getIpGeolocation() = resultFlow {
		ipApi.lookup()
	}.flowOn(ioDispatcher)

	fun search(query: String) = resultFlow {
		when {
			query.isEmpty() -> emptyList()
			query.matches(coordinateRegex) -> {
				val (lat, lng) = query.split(',')
				val resp = weatherApi.find(lat, lng)
				responseToModelList(resp)
			}
			else -> {
				val resp = weatherApi.find(query)
				responseToModelList(resp)
			}
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