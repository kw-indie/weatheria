package asceapps.weatheria.data.repo

import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import asceapps.weatheria.data.api.OneCallResponse
import asceapps.weatheria.data.api.WeatherService
import asceapps.weatheria.data.dao.WeatherInfoDao
import asceapps.weatheria.data.entity.CurrentEntity
import asceapps.weatheria.data.entity.DailyEntity
import asceapps.weatheria.data.entity.HourlyEntity
import asceapps.weatheria.data.entity.LocationEntity
import asceapps.weatheria.data.entity.SavedLocationEntity
import asceapps.weatheria.data.entity.WeatherInfoEntity
import asceapps.weatheria.model.Current
import asceapps.weatheria.model.Daily
import asceapps.weatheria.model.Hourly
import asceapps.weatheria.model.Location
import asceapps.weatheria.model.WeatherInfo
import asceapps.weatheria.util.conditionIcon
import asceapps.weatheria.util.conditionIndex
import asceapps.weatheria.util.dirIndex
import asceapps.weatheria.util.toInstant
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class WeatherInfoRepo @Inject constructor(
	private val service: WeatherService,
	private val dao: WeatherInfoDao
) {

	fun getAll() = dao.loadAll()
		.distinctUntilChanged()
		.map {list -> list.map {entityToModel(it)}}

	fun getSavedLocations() = dao.loadSavedLocations()
		.distinctUntilChanged()

	fun get(locationId: Int) = dao.load(locationId)
		.distinctUntilChanged()

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

	suspend fun refresh(id: Int, lat: Float, lng: Float) {
		val oneCallResp = service.oneCall(lat.toString(), lng.toString())
		val current = extractCurrentEntity(id, oneCallResp)
		val hourly = extractHourlyEntity(id, oneCallResp)
		val daily = extractDailyEntity(id, oneCallResp)
		dao.update(current, hourly, daily)
	}

	suspend fun refreshAll() {
		dao.savedLocations().forEach {
			with(it) {refresh(id, lat, lng)}
		}
	}

	suspend fun reorder(l: SavedLocationEntity, toPos: Int) = dao.reorder(l, toPos)

	suspend fun delete(l: SavedLocationEntity) = dao.delete(l.id, l.pos)

	suspend fun delete(l: Location) = dao.delete(l.id, l.order)

	suspend fun retain(l: Location) = dao.retain(l.id)

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

	private fun entityToModel(info: WeatherInfoEntity): WeatherInfo {
		val location = with(info.location) {
			Location(id, lat, lng, name, country,
				ZoneOffset.ofTotalSeconds(info.location.zoneOffset),
				pos
			)
		}
		val daily = info.daily.map {
			with(it) {
				Daily(
					dt.toInstant(),
					sunrise.toInstant(),
					sunset.toInstant(),
					conditionIcon(conditionId),
					minTemp,
					maxTemp,
					pop,
					uvi
				)
			}
		}
		val first3DaysDaytime = arrayListOf(
			with(daily[0]) {sunrise..sunset},
			with(daily[1]) {sunrise..sunset},
			with(daily[2]) {sunrise..sunset}
		)
		val hourly = info.hourly.map {
			with(it) {
				val hour = dt.toInstant()
				// hourly data are only from first 48 hours, starting from this hour not 00:00
				Hourly(
					hour,
					conditionIcon(conditionId, first3DaysDaytime.any {daytime -> hour in daytime}),
					temp,
					pop
				)
			}
		}
		val lastUpdate = info.current.dt.toInstant()
		val now = Instant.now()
		val elapsedTime = Duration.between(lastUpdate, now)
		val current = when {
			elapsedTime.toHours() < 1 -> { // fresh
				with(info.current) {
					Current(
						conditionIndex(conditionId),
						conditionIcon(conditionId, now in first3DaysDaytime[0]),
						temp,
						feelsLike,
						pressure,
						humidity,
						dewPoint,
						clouds,
						visibility,
						windSpeed,
						dirIndex(windDir)
					)
				}
			}
			elapsedTime.toHours() < info.hourly.size -> { // approximate from hourly
				with(info.hourly.last {it.dt < now.epochSecond}) {
					Current(
						conditionIndex(conditionId),
						conditionIcon(conditionId, first3DaysDaytime.any {daytime -> now in daytime}),
						temp,
						feelsLike,
						pressure,
						humidity,
						dewPoint,
						clouds,
						visibility,
						windSpeed,
						dirIndex(windDir),
						true
					)
				}
			}
			else -> { // approximate from daily, if no match, just use last day
				val day = info.daily.last {it.dt < now.epochSecond}
				// we can push all times to offset, but we'll get the same result with default offset
				val nowTime = LocalTime.now()
				val morn = LocalTime.of(6, 0)
				val noon = LocalTime.of(12, 0)
				val eve = LocalTime.of(18, 0)
				with(day) {
					Current(
						conditionIndex(conditionId),
						conditionIcon(conditionId, now.epochSecond in sunrise..sunset),
						when {
							nowTime.isBefore(morn) -> mornTemp
							nowTime.isBefore(noon) -> dayTemp
							nowTime.isBefore(eve) -> eveTemp
							else -> nightTemp
						},
						when {
							nowTime.isBefore(morn) -> mornFeel
							nowTime.isBefore(noon) -> dayFeel
							nowTime.isBefore(eve) -> eveFeel
							else -> nightFeel
						},
						pressure,
						humidity,
						dewPoint,
						clouds,
						info.hourly.last().visibility, // no visibility in daily, use last hour's
						windSpeed,
						dirIndex(windDir),
						true
					)
				}
			}
		}
		return WeatherInfo(location, lastUpdate, current, hourly, daily)
	}
}