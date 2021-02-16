package asceapps.weatheria.data.repo

import asceapps.weatheria.data.api.OneCallResponse
import asceapps.weatheria.data.api.WeatherService
import asceapps.weatheria.data.dao.WeatherInfoDao
import asceapps.weatheria.data.entity.CurrentEntity
import asceapps.weatheria.data.entity.DailyEntity
import asceapps.weatheria.data.entity.HourlyEntity
import asceapps.weatheria.data.entity.LocationEntity
import asceapps.weatheria.data.entity.SavedLocationEntity
import asceapps.weatheria.data.entity.WeatherInfoEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class WeatherInfoRepo @Inject constructor(
	private val service: WeatherService,
	private val dao: WeatherInfoDao
) {

	fun loadAll() = dao.loadAll()

	fun loadAllIds() = dao.loadAllIds()

	fun load(locationId: Int) = dao.load(locationId)

	suspend fun fetch(l: LocationEntity) {
		val oneCallResp = with(l) {
			service.oneCall(lat.toString(), lng.toString())
		}
		with(oneCallToWeatherInfoEntity(l, oneCallResp)) {
			dao.insert(location, current, hourly, daily)
		}
		// no need to return, as updating db will trigger live data
	}

	suspend fun refresh(id: Int, lat: Float, lng: Float) {
		val oneCallResp = service.oneCall(lat.toString(), lng.toString())
		val current = extractCurrentEntity(id, oneCallResp)
		val hourly = extractHourlyEntity(id, oneCallResp)
		val daily = extractDailyEntity(id, oneCallResp)
		dao.update(current, hourly, daily)
	}

	suspend fun refreshAll() {
		dao.getSavedLocations().forEach {
			with(it) {refresh(id, lat, lng)}
		}
	}

	suspend fun reorder(id: Int, fromPos: Int, toPos: Int) = dao.reorder(id, fromPos, toPos)

	suspend fun delete(id: Int, pos: Int) = dao.delete(id, pos)

	suspend fun deleteAll() = dao.deleteAll()

	private fun oneCallToWeatherInfoEntity(l: LocationEntity, resp: OneCallResponse): WeatherInfoEntity {
		val savedLocation = with(l) {
			SavedLocationEntity(id, lat, lng, name, country, resp.timezone_offset)
		}
		// if we are parsing with a found location, all nullables are non-null
		val current = extractCurrentEntity(l.id, resp)
		val hourly = extractHourlyEntity(l.id, resp)
		val daily = extractDailyEntity(l.id, resp)
		return WeatherInfoEntity(savedLocation, current, hourly, daily)
	}

	private fun extractCurrentEntity(locationId: Int, resp: OneCallResponse) = with(resp.current) {
		CurrentEntity(
			locationId,
			dt,
			weather[0].id,
			wind_speed,
			wind_deg,
			pressure,
			humidity,
			dew_point.roundToInt(),
			clouds,
			temp.roundToInt(),
			feels_like.roundToInt(),
			visibility,
			rain?._1h,
			snow?._1h
		)
	}

	private fun extractHourlyEntity(locationId: Int, resp: OneCallResponse) = resp.hourly.map {
		with(it) {
			val weather = weather[0]
			HourlyEntity(
				locationId,
				dt,
				weather.id,
				wind_speed,
				wind_deg,
				pressure,
				humidity,
				dew_point.roundToInt(),
				clouds,
				temp.roundToInt(),
				feels_like.roundToInt(),
				visibility,
				(pop * 100).roundToInt(),
				rain?._1h,
				snow?._1h
			)
		}
	}

	private fun extractDailyEntity(locationId: Int, resp: OneCallResponse) = resp.daily.map {
		with(it) {
			val weather = weather[0]
			val temp = it.temp
			val feel = it.feels_like
			DailyEntity(
				locationId,
				dt,
				weather.id,
				wind_speed,
				wind_deg,
				pressure,
				humidity,
				dew_point.roundToInt(),
				clouds,
				temp.min.roundToInt(),
				temp.max.roundToInt(),
				temp.morn.roundToInt(),
				temp.day.roundToInt(),
				temp.eve.roundToInt(),
				temp.night.roundToInt(),
				feel.morn.roundToInt(),
				feel.day.roundToInt(),
				feel.eve.roundToInt(),
				feel.night.roundToInt(),
				sunrise,
				sunset,
				(pop * 100).roundToInt(),
				uvi,
				rain,
				snow
			)
		}
	}
}