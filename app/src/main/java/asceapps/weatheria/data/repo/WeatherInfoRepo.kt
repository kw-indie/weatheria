package asceapps.weatheria.data.repo

import asceapps.weatheria.data.api.FindResponse
import asceapps.weatheria.data.api.OneCallResponse
import asceapps.weatheria.data.api.WeatherService
import asceapps.weatheria.data.dao.WeatherInfoDao
import asceapps.weatheria.data.entity.*
import asceapps.weatheria.di.IoDispatcher
import asceapps.weatheria.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.time.*
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class WeatherInfoRepo @Inject constructor(
	private val service: WeatherService,
	private val dao: WeatherInfoDao,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

	fun getAll() = flow {
		emit(Result.Loading)
		val flow = dao.loadAll()
			.map { list ->
				val modList = list.map { e -> entityToModel(e) }
				Result.Success(modList)
			}
			.flowOn(ioDispatcher)
		emitAll(flow)
	}

	fun getAllIds() = flow {
		emit(Result.Loading)
		val flow = dao.loadAllIds()
			.distinctUntilChanged()
			.map { Result.Success(it) }
		emitAll(flow)
	}

	fun get(locationId: Int) = flow {
		emit(Result.Loading)
		val flow = dao.load(locationId)
			.distinctUntilChanged()
			.map { Result.Success(it) }
		emitAll(flow)
	}

	suspend fun get(l: FindResponse.Location) {
		withContext(ioDispatcher) {
			val oneCallResp = with(l) {
				service.oneCall(coord.lat.toString(), coord.lon.toString())
			}
			with(responsesToEntity(l, oneCallResp)) {
				dao.insert(location, current, hourly, daily)
			}
			// no need to return, as updating db will trigger new emission on [all]
		}
	}

	fun search(query: String) = flow {
		emit(Result.Loading)
		// todo catch errors
		when {
			query.isEmpty() -> emit(Result.Success(emptyList<FindResponse.Location>()))
			query.matches(coordinateRegex) -> {
				val (lat, lng) = query.split(',')
				val list = service.find(lat, lng).list
				emit(Result.Success(list))
			}
			else -> {
				val list = service.find(query).list
				emit(Result.Success(list))
			}
		}
	}

	suspend fun refresh(id: Int, lat: Float, lng: Float) {
		withContext(ioDispatcher) {
			val oneCallResp = service.oneCall(lat.toString(), lng.toString())
			val current = extractCurrentEntity(id, oneCallResp)
			val hourly = extractHourlyEntity(id, oneCallResp)
			val daily = extractDailyEntity(id, oneCallResp)
			dao.update(current, hourly, daily)
		}
	}

	suspend fun refreshAll() {
		withContext(ioDispatcher) {
			dao.getLocations().forEach {
				with(it) { refresh(id, lat, lng) }
			}
		}
	}

	suspend fun reorder(id: Int, fromPos: Int, toPos: Int) {
		withContext(ioDispatcher) {
			dao.reorder(id, fromPos, toPos)
		}
	}

	suspend fun delete(id: Int, pos: Int) {
		withContext(ioDispatcher) {
			dao.delete(id, pos)
		}
	}

	suspend fun deleteAll() {
		withContext(ioDispatcher) {
			dao.deleteAll()
		}
	}

	companion object {

		private val coordinateRegex =
			Regex("^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?)\\s*,\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)\$")

		private fun responsesToEntity(
			l: FindResponse.Location,
			ocr: OneCallResponse
		): WeatherInfoEntity {
			val location = with(l) {
				LocationEntity(id, coord.lat, coord.lon, name, sys.country, ocr.timezone_offset)
			}
			val current = extractCurrentEntity(l.id, ocr)
			val hourly = extractHourlyEntity(l.id, ocr)
			val daily = extractDailyEntity(l.id, ocr)
			return WeatherInfoEntity(location, current, hourly, daily)
		}

		private fun extractCurrentEntity(locationId: Int, resp: OneCallResponse) =
			with(resp.current) {
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
				Location(
					id, lat, lng, name, country,
					ZoneOffset.ofTotalSeconds(info.location.zoneOffset),
					pos
				)
			}
			val daily = info.daily.map {
				with(it) {
					Daily(
						toInstant(dt),
						toInstant(sunrise),
						toInstant(sunset),
						conditionIcon(conditionId),
						minTemp,
						maxTemp,
						pop,
						uvi
					)
				}
			}
			val first3DaysDaytime = arrayListOf(
				with(daily[0]) { sunrise..sunset },
				with(daily[1]) { sunrise..sunset },
				with(daily[2]) { sunrise..sunset }
			)
			val hourly = info.hourly.map {
				with(it) {
					val hour = toInstant(dt)
					// hourly data are only from first 48 hours, starting from this hour not 00:00
					Hourly(
						hour,
						conditionIcon(
							conditionId,
							first3DaysDaytime.any { daytime -> hour in daytime }),
						temp,
						pop
					)
				}
			}
			val lastUpdate = toInstant(info.current.dt)
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
							dirIndex(windDir),
							0 // high
						)
					}
				}
				elapsedTime.toHours() < info.hourly.size -> { // approximate from hourly
					with(info.hourly.last { it.dt < now.epochSecond }) {
						Current(
							conditionIndex(conditionId),
							conditionIcon(
								conditionId,
								first3DaysDaytime.any { daytime -> now in daytime }),
							temp,
							feelsLike,
							pressure,
							humidity,
							dewPoint,
							clouds,
							visibility,
							windSpeed,
							dirIndex(windDir),
							1 // medium
						)
					}
				}
				else -> { // approximate from daily, if no match, just use last day
					val day = info.daily.last { it.dt < now.epochSecond }
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
							2 // low
						)
					}
				}
			}
			return WeatherInfo(location, lastUpdate, current, hourly, daily)
		}

		private fun toInstant(epochSeconds: Int) = Instant.ofEpochSecond(epochSeconds.toLong())

		// region weather condition stuff
		private val conditionIds = intArrayOf(
			200,
			201,
			202,
			210,
			211,
			212,
			221,
			230,
			231,
			232,
			300,
			301,
			302,
			310,
			311,
			312,
			313,
			314,
			321,
			500,
			501,
			502,
			503,
			504,
			511,
			520,
			521,
			522,
			531,
			600,
			601,
			602,
			611,
			612,
			613,
			615,
			616,
			620,
			621,
			622,
			701,
			711,
			721,
			731,
			741,
			751,
			761,
			762,
			771,
			781,
			800,
			801,
			802,
			803,
			804
		)

		private fun conditionIndex(conditionId: Int) = conditionIds.binarySearch(conditionId)

		private fun conditionIcon(conditionId: Int, isDay: Boolean? = null) =
			when (conditionId) {
				in 200..232 -> "11" // thunderstorm
				in 300..321 -> "09" // drizzle
				in 500..504 -> "10" // rain
				511 -> "13" // freezing rain
				in 520..531 -> "09" // showers
				in 600..622 -> "13" // snow
				in 700..781 -> "50" // atmosphere
				800 -> "01" // clear sky
				801 -> "02" // few clouds
				802 -> "03" // scattered clouds
				803 -> "04" // broken clouds
				804 -> "04" // overcast clouds
				else -> throw IllegalArgumentException("no such condition")
			} + when (isDay) {
				true -> "d"
				false -> "n"
				else -> ""
			}
		// endregion

		private fun dirIndex(deg: Int) = when ((deg + 22) % 360) {
			in 0..44 -> 0 // E
			in 45..89 -> 1 // NE
			in 90..134 -> 2 // N
			in 135..179 -> 3 // NW
			in 180..224 -> 4 // W
			in 225..269 -> 5 // SW
			in 270..314 -> 6 // S
			else -> 7 // SE
		}

		private fun moonPhase(instant: Instant, offset: ZoneOffset): Int {
			val day = HijrahDate.from(
				OffsetDateTime.ofInstant(instant, offset)
			)[ChronoField.DAY_OF_MONTH]
			val phase = when (day) {
				in 2..6 -> 0 //"Waxing Crescent Moon"
				in 6..8 -> 1 //"Quarter Moon"
				in 8..13 -> 2 //"Waxing Gibbous Moon"
				in 13..15 -> 3 //"Full Moon"
				in 15..21 -> 4 //"Waning Gibbous Moon"
				in 21..23 -> 5 //"Last Quarter Moon"
				in 23..28 -> 6 //"Waning Crescent Moon"
				else -> 7 //"New Moon" includes 28.53-29.5 and 0-1
			}
			return phase
		}

		private fun moonPhase2(instant: Instant, offset: ZoneOffset): Int {
			// lunar cycle days
			val lunarCycle = 29.530588853
			// a reference new moon
			val ref = LocalDateTime.of(2000, 1, 6, 18, 14).atOffset(offset)
			// could ask for hour/min for a tiny bit of extra accuracy
			val now = OffsetDateTime.ofInstant(instant, offset)
			// this loses a number of hours of accuracy
			val days = ChronoUnit.DAYS.between(ref, now)
			val cycles = days / lunarCycle
			// take fractional part of cycles x full cycle = current lunation
			val lunation = (cycles % 1) * lunarCycle
			return when (lunation) {
				in 1.0..6.38 -> 0 //"Waxing Crescent Moon"
				in 6.38..8.38 -> 1 //"Quarter Moon"
				in 8.38..13.765 -> 2 //"Waxing Gibbous Moon"
				in 13.765..15.765 -> 3 //"Full Moon"
				in 15.765..21.148 -> 4 //"Waning Gibbous Moon"
				in 21.148..23.148 -> 5 //"Last Quarter Moon"
				in 23.148..28.53 -> 6 //"Waning Crescent Moon"
				else -> 7 //"New Moon" includes 28.53-29.5 and 0-1
			}
		}
	}
}