package asceapps.weatheria.model

import android.text.format.DateUtils
import asceapps.weatheria.api.OneCallResponse
import asceapps.weatheria.api.WeatherService
import asceapps.weatheria.db.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Instant.ofEpochSecond
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class WeatherInfoRepo @Inject constructor(
	private val service: WeatherService,
	private val dao: WeatherInfoDao
) {

	fun getAllInfo() = dao.getAllInfo()
		.distinctUntilChanged()
		.map {list -> list.map {entityToWeatherInfo(it)}}

	fun getSavedLocations() = dao.getSavedLocations()

	fun getInfo(locationId: Int) = dao.getInfo(locationId)

	suspend fun find(lat: Float, lng: Float, accuracy: Int = 1) = dao.find(
		lat, lng,
		lat - accuracy,
		lat + accuracy,
		lng - accuracy,
		lng + accuracy
	)

	suspend fun find(locationName: String) = dao.find(locationName)

	suspend fun fetch(l: LocationEntity) {
		// todo see if we have a hit in db, if we do, show error/update
		val oneCallResp = with(l) {
			service.oneCall(lat.toString(), lng.toString())
		}
		with(oneCallToWeatherInfoEntity(l, oneCallResp)) {
			dao.insert(location, current, hourly, daily)
		}
		// no need to return as updating db will trigger live data
	}

	suspend fun get(lat: String, lng: String) {
		// todo search locally first
		val foundLocations = find(lat.toFloat(), lng.toFloat())
		// todo let user choose a location
		fetch(foundLocations[0])
	}

	suspend fun get(query: String) {
		// todo search locally first
		// todo reuse code from above
		val foundLocations = find(query)
		// todo let user choose a location
		fetch(foundLocations[0])
	}

	suspend fun getUpdate(l: Location) {
		val oneCallResp = service.oneCall(l.lat.toString(), l.lng.toString())
		val current = extractCurrentEntity(l.id, oneCallResp)
		val hourly = extractHourlyEntity(l.id, oneCallResp)
		val daily = extractDailyEntity(l.id, oneCallResp)
		dao.update(current, hourly, daily)
	}

	suspend fun delete(l: Location) = dao.delete(l.id, l.order)

	suspend fun deleteAll() = dao.deleteAll()

	private fun oneCallToWeatherInfoEntity(l: LocationEntity, resp: OneCallResponse): WeatherInfoEntity {
		val savedLocation = with(l) {
			SavedLocationEntity(id, lat, lng, name, country, resp.timezone_offset)
		}
		// if we are parsing with a found location, all nullables are non-null
		val current = extractCurrentEntity(savedLocation.id, resp)
		val hourly = extractHourlyEntity(savedLocation.id, resp)
		val daily = extractDailyEntity(savedLocation.id, resp)
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

	private fun entityToWeatherInfo(info: WeatherInfoEntity): WeatherInfo {
		val zoneOffset = ZoneOffset.ofTotalSeconds(info.location.zoneOffset)
		val location = with(info.location) {
			Location(id, lat, lng, name, country, zoneOffset, pos)
		}
		val daily = info.daily.map {
			with(it) {
				Daily(
					OffsetDateTime.ofInstant(ofEpochSecond(dt.toLong()), zoneOffset).toLocalDate(),
					OffsetTime.ofInstant(ofEpochSecond(sunrise.toLong()), zoneOffset).toLocalTime(),
					OffsetTime.ofInstant(ofEpochSecond(sunset.toLong()), zoneOffset).toLocalTime(),
					iconOf(conditionId), // no d or n, cuz we will prolly use both in ui
					minTemp,
					maxTemp,
					pop,
					uvi
				)
			}
		}
		val now = OffsetDateTime.now(ZoneId.from(location.zoneOffset))
		val todayWeather = now.toLocalDate().let {nowDate ->
			daily.find {it.date == nowDate}
		}
		val hourly = info.hourly.map {
			with(it) {
				val time = OffsetTime.ofInstant(ofEpochSecond(dt.toLong()), zoneOffset).toLocalTime()
				val isThisHourDaytime = todayWeather?.run {time in sunrise..sunset}
				Hourly(
					time,
					// this returns an accurate result for today, and a good approximation for tomorrow
					// since max difference in sunset/rise won't exceed a few minutes
					iconOf(conditionId, isThisHourDaytime),
					temp,
					pop
				)
			}
		}
		val lastUpdate = DateUtils.getRelativeTimeSpanString(info.current.dt * 1000L)
		val thisHour = now.toLocalTime().truncatedTo(ChronoUnit.HOURS).let {nowHour ->
			hourly.find {it.time == nowHour}
		}
		val isNowDaytime = now.toLocalTime().let {nowTime ->
			todayWeather?.run {nowTime in sunrise..sunset}
		}
		// todo offer approximation when out of date
		val current = with(info.current) {
			Current(
				conditionId,
				iconOf(conditionId, isNowDaytime),
				temp,
				feelsLike,
				pressure,
				humidity,
				dewPoint,
				clouds,
				visibility,
				windSpeed,
				windDir
			)
		}
		return WeatherInfo(
			location, current, hourly, daily,
			lastUpdate, now, todayWeather, thisHour, isNowDaytime
		)
	}
}