package asceapps.weatheria.data.repo

import asceapps.weatheria.data.api.OneCallResponse
import asceapps.weatheria.data.api.WeatherApi
import asceapps.weatheria.data.base.BaseLocation
import asceapps.weatheria.data.dao.WeatherInfoDao
import asceapps.weatheria.data.entity.CurrentEntity
import asceapps.weatheria.data.entity.DailyEntity
import asceapps.weatheria.data.entity.HourlyEntity
import asceapps.weatheria.data.entity.LocationEntity
import asceapps.weatheria.data.entity.WeatherInfoEntity
import asceapps.weatheria.data.model.Current
import asceapps.weatheria.data.model.Daily
import asceapps.weatheria.data.model.FoundLocation
import asceapps.weatheria.data.model.Hourly
import asceapps.weatheria.data.model.Location
import asceapps.weatheria.data.model.WeatherInfo
import asceapps.weatheria.di.IoDispatcher
import asceapps.weatheria.util.asResult
import asceapps.weatheria.util.resultFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class WeatherInfoRepo @Inject constructor(
	private val api: WeatherApi,
	private val dao: WeatherInfoDao,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

	fun getAll() = dao.loadAll()
		.map { list -> list.map { e -> entityToModel(e) } }
		.asResult() // concerned over 2 consecutive Flow.map()'s
		.flowOn(ioDispatcher)

	// todo move to locationRepo? add location there, then when this repo does not find info, make it fetch
	fun add(fl: FoundLocation) = resultFlow<Unit> {
		val ocr = withContext(ioDispatcher) {
			api.oneCall(fl.lat.toString(), fl.lng.toString())
		}
		val pos = dao.getLocationsCount()
		val l = with(fl) {
			LocationEntity(id, lat, lng, name, country, ocr.timezone_offset, pos)
		}
		val c = extractCurrent(fl.id, ocr)
		val h = extractHourly(fl.id, ocr)
		val d = extractDaily(fl.id, ocr)
		val infoEntity = WeatherInfoEntity(l, c, h, d)
		dao.insert(infoEntity)
	}

	private suspend fun internalRefresh(loc: BaseLocation) {
		val ocr = withContext(ioDispatcher) {
			api.oneCall(loc.lat.toString(), loc.lng.toString())
		}
		val c = extractCurrent(loc.id, ocr)
		val h = extractHourly(loc.id, ocr)
		val d = extractDaily(loc.id, ocr)
		dao.update(ocr.timezone_offset, c, h, d)
	}

	fun refresh(info: WeatherInfo) = resultFlow<Unit> {
		internalRefresh(info.location)
	}

	fun refreshAll() = resultFlow<Unit> {
		val locations = dao.getLocations()
		val size = locations.size
		locations.forEachIndexed { i, e ->
			internalRefresh(e)
			emit(Loading(100 / size * (i + 1)))
		}
	}

	suspend fun reorder(info: WeatherInfo, toPos: Int) {
		with(info.location) { dao.reorder(id, pos, toPos) }
	}

	suspend fun delete(info: WeatherInfo) {
		with(info.location) { dao.delete(id, pos) }
	}

	suspend fun deleteAll() {
		dao.deleteAll()
	}

	companion object {

		private fun extractCurrent(locationId: Int, ocr: OneCallResponse) = with(ocr.current) {
			CurrentEntity(
				locationId,
				dt,
				weather[0].id,
				wind_speed,
				meteorologicalToCircular(wind_deg),
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

		private fun extractHourly(locationId: Int, ocr: OneCallResponse) = ocr.hourly.map {
			with(it) {
				val weather = weather[0]
				HourlyEntity(
					locationId,
					dt,
					weather.id,
					wind_speed,
					meteorologicalToCircular(wind_deg),
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

		private fun extractDaily(locationId: Int, ocr: OneCallResponse) = ocr.daily.map {
			with(it) {
				val weather = weather[0]
				val temp = it.temp
				val feel = it.feels_like
				DailyEntity(
					locationId,
					dt,
					weather.id,
					wind_speed,
					meteorologicalToCircular(wind_deg),
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
					id,
					lat,
					lng,
					name,
					countryCodeToFlag(country),
					ZoneOffset.ofTotalSeconds(zoneOffset),
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
						conditionIcon(conditionId, first3DaysDaytime.any { daytime -> hour in daytime }),
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
							toInstant(dt),
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
							windDir,
							0 // high
						)
					}
				}
				elapsedTime.toHours() < info.hourly.size -> { // approximate from hourly
					with(info.hourly.last { it.dt < now.epochSecond }) {
						Current(
							toInstant(dt),
							conditionIndex(conditionId),
							conditionIcon(conditionId, first3DaysDaytime.any { daytime -> now in daytime }),
							temp,
							feelsLike,
							pressure,
							humidity,
							dewPoint,
							clouds,
							visibility,
							windSpeed,
							windDir,
							1 // medium
						)
					}
				}
				else -> { // approximate from daily, if no match, just use last day
					val day = info.daily.last { it.dt < now.epochSecond }
					// we can push all times to offset, but we'll get the same result with default offset
					val nowTime = LocalTime.now()
					// these times are all subjective, we could mean one thing but the api could mean another
					val morn = LocalTime.of(6, 0)
					val noon = morn.plusHours(6)
					val eve = noon.plusHours(6)
					with(day) {
						Current(
							toInstant(dt),
							conditionIndex(conditionId),
							conditionIcon(conditionId, now.epochSecond in sunrise..sunset),
							when {
								nowTime < morn -> mornTemp
								nowTime < noon -> dayTemp
								nowTime < eve -> eveTemp
								else -> nightTemp
							},
							when {
								nowTime < morn -> mornFeel
								nowTime < noon -> dayFeel
								nowTime < eve -> eveFeel
								else -> nightFeel
							},
							pressure,
							humidity,
							dewPoint,
							clouds,
							// no visibility in daily, use last hour's
							info.hourly.last().visibility,
							windSpeed,
							windDir,
							2 // low
						)
					}
				}
			}
			return WeatherInfo(location, lastUpdate, current, hourly, daily)
		}

		private fun toInstant(epochSeconds: Int) = Instant.ofEpochSecond(epochSeconds.toLong())

		private fun meteorologicalToCircular(deg: Int) = (-deg + 90).mod(360)

		private fun countryCodeToFlag(cc: String): String {
			val offset = 0x1F1E6 - 0x41 // tiny A - normal A
			val cps = IntArray(2) { i -> cc[i].code + offset }
			return String(cps, 0, cps.size)
		}

		// region weather condition stuff
		private val conditionIds = intArrayOf(
			200, 201, 202, 210, 211, 212, 221, 230, 231, 232, 300, 301, 302, 310, 311, 312, 313, 314, 321,
			500, 501, 502, 503, 504, 511, 520, 521, 522, 531, 600, 601, 602, 611, 612, 613, 615, 616, 620,
			621, 622, 701, 711, 721, 731, 741, 751, 761, 762, 771, 781, 800, 801, 802, 803, 804
		)

		private fun conditionIndex(conditionId: Int) = conditionIds.binarySearch(conditionId)

		private fun conditionIcon(conditionId: Int, isDay: Boolean? = null) =
			when(conditionId) {
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
			} + when(isDay) {
				true -> "d"
				false -> "n"
				else -> ""
			}
		// endregion

		private fun moonPhase(instant: Instant, offset: ZoneOffset): Int {
			val day = HijrahDate.from(OffsetDateTime.ofInstant(instant, offset))[ChronoField.DAY_OF_MONTH]
			val phase = when(day) {
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
			return when(lunation) {
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