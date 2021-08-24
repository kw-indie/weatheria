package asceapps.weatheria.data.repo

import asceapps.weatheria.R
import asceapps.weatheria.data.api.ForecastResponse
import asceapps.weatheria.data.api.SearchResponse
import asceapps.weatheria.data.api.WeatherApi
import asceapps.weatheria.data.base.BaseLocation
import asceapps.weatheria.data.dao.WeatherInfoDao
import asceapps.weatheria.data.entity.*
import asceapps.weatheria.data.model.*
import asceapps.weatheria.di.IoDispatcher
import asceapps.weatheria.util.asResult
import asceapps.weatheria.util.resultFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.*
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
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

	fun get(locationId: Int) = dao.get(locationId)
		.map { e -> entityToModel(e) }
		.asResult()
		.flowOn(ioDispatcher)

	fun getByPos(pos: Int) = dao.getByPos(pos)
		.map { e -> entityToModel(e) }
		.asResult()
		.flowOn(ioDispatcher)

	fun search(q: String? = null) = resultFlow<List<SearchResponse>> {
		api.search(if(q.isNullOrBlank()) WeatherApi.AUTO_IP else q)
	}

	private suspend fun fetchForecast(q: String, days: Int = 10) = api.forecast(q, days)

	fun add(loc: SearchResponse) = resultFlow<Unit> {
		val resp = withContext(ioDispatcher) {
			fetchForecast(loc.searchTerm)
		}
		val l = with(resp.location) {
			val lastUpdate = (System.currentTimeMillis() / 1000).toInt()
			val pos = dao.getLocationsCount()
			LocationEntity(loc.id, lat, lng, name, country, zoneId, lastUpdate, pos)
		}
		val c = extractCurrent(loc.id, resp)
		val h = extractHourly(loc.id, resp)
		val d = extractDaily(loc.id, resp)
		dao.insert(l, c, h, d)
	}

	private suspend fun internalRefresh(loc: BaseLocation) {
		val resp = withContext(ioDispatcher) {
			fetchForecast(loc.searchTerm)
		}
		val lastUpdate = (System.currentTimeMillis() / 1000).toInt()
		val c = extractCurrent(loc.id, resp)
		val h = extractHourly(loc.id, resp)
		val d = extractDaily(loc.id, resp)
		dao.update(lastUpdate, c, h, d)
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

		private fun extractCurrent(locationId: Int, resp: ForecastResponse) = with(resp.current) {
			val firstHour = resp.forecastDays[0].hourly[0]
			CurrentEntity(
				locationId,
				dt,
				temp_c.roundToInt(),
				feelsLike_c.roundToInt(),
				// save condition index to reduce later work
				condition,
				isDay == 1,
				wind_kph,
				meteorologicalToCircular(wind_degree),
				pressure_mb.roundToInt(),
				precip_mm.roundToInt(),
				humidity,
				firstHour.dewPoint_c.roundToInt(),
				clouds,
				vis_km,
				firstHour.uv.roundToInt()
			)
		}

		private fun extractHourly(locationId: Int, resp: ForecastResponse) =
			resp.forecastDays.flatMap { forecastDay ->
				forecastDay.hourly.map { hour ->
					with(hour) {
						HourlyEntity(
							locationId,
							dt,
							temp_c.roundToInt(),
							feelsLike_c.roundToInt(),
							condition,
							isDay == 1,
							wind_kph,
							meteorologicalToCircular(wind_degree),
							pressure_mb.roundToInt(),
							precip_mm.roundToInt(),
							humidity,
							dewPoint_c.roundToInt(),
							clouds,
							vis_km,
							max(chanceOfRain, chanceOfSnow),
							uv.roundToInt()
						)
					}
				}
			}

		private fun extractDaily(locationId: Int, resp: ForecastResponse) =
			resp.forecastDays.map { day ->
				val zone = ZoneId.of(resp.location.zoneId)
				with(day) {
					DailyEntity(
						locationId,
						dt,
						minTemp_c.roundToInt(),
						maxTemp_c.roundToInt(),
						condition,
						wind_kph,
						precip_mm.roundToInt(),
						humidity,
						vis_km,
						max(chanceOfRain, chanceOfSnow),
						uv.roundToInt(),
						localTimeToEpochSeconds(dt, sunrise, zone),
						localTimeToEpochSeconds(dt, sunset, zone),
						localTimeToEpochSeconds(dt, moonrise, zone),
						localTimeToEpochSeconds(dt, moonset, zone),
						moonPhaseIndex(moonPhase)
					)
				}
			}

		private fun entityToModel(info: WeatherInfoEntity): WeatherInfo {
			val location = with(info.location) {
				Location(id, lat, lng, name, country, ZoneId.of(zoneId), toInstant(lastUpdate), pos)
			}
			val daily = info.daily.map {
				with(it) {
					Daily(
						toInstant(dt),
						minTemp,
						maxTemp,
						conditionIconResId(condition),
						pop,
						uv,
						toInstant(sunrise),
						toInstant(sunset),
						toInstant(moonrise),
						toInstant(moonset),
						moonphase
					)
				}
			}
			val hourly = info.hourly.map {
				with(it) {
					val hour = toInstant(dt)
					// hourly data are only from first 48 hours, starting from this hour not 00:00
					Hourly(
						hour,
						conditionIconResId(condition, isDay),
						temp,
						pop
					)
				}
			}
			val now = Instant.now()
			val elapsedHours = Duration.between(location.lastUpdate, now).toHours()
			val accuracy = when {
				elapsedHours < 1 -> 0
				elapsedHours < 24 -> 1
				elapsedHours < 48 -> 2
				elapsedHours < 72 -> 3
				else -> 4 // no data to approximate from
			}
			val current = if(accuracy == 0) {
				with(info.current) {
					Current(
						conditionIndex(condition),
						conditionIconResId(condition, isDay),
						temp,
						feelsLike,
						pressure,
						humidity,
						dewPoint,
						clouds,
						visibility,
						windSpeed,
						windDir,
						uv
					)
				}
			} else {
				// approximation logic: we have hourly data for the next 3 days
				val hour = info.hourly.last { it.dt < now.epochSecond }
				with(hour) {
					Current(
						conditionIndex(condition),
						conditionIconResId(condition, isDay),
						temp,
						feelsLike,
						pressure,
						humidity,
						dewPoint,
						clouds,
						visibility,
						windSpeed,
						windDir,
						uv
					)
				}
			}
			return WeatherInfo(location, current, hourly, daily, accuracy)
		}

		private fun toInstant(epochSeconds: Int) = Instant.ofEpochSecond(epochSeconds.toLong())

		private fun conditionIndex(condition: Int) = WeatherApi.CONDITIONS.binarySearch(condition)

		private fun conditionIconResId(condition: Int, isDay: Boolean? = null) = when(condition) {
			1000 -> if(isDay == false) R.drawable.w_clear_n else R.drawable.w_clear_d
			1003 -> if(isDay == false) R.drawable.w_cloudy_p_n else R.drawable.w_cloudy_p_d
			1006 -> if(isDay == false) R.drawable.w_cloudy_n else R.drawable.w_cloudy_d
			1009 -> R.drawable.w_overcast
			1030 -> R.drawable.w_mist
			1063, 1180, 1183, 1186, 1189, 1240 -> when(isDay) {
				true -> R.drawable.w_rain_l_d
				false -> R.drawable.w_rain_l_n
				else -> R.drawable.w_rain_l
			}
			1066, 1210, 1213, 1216, 1219, 1255 -> when(isDay) {
				true -> R.drawable.w_snow_l_d
				false -> R.drawable.w_snow_l_n
				else -> R.drawable.w_snow_l
			}
			1069, 1204, 1207, 1249 -> when(isDay) {
				true -> R.drawable.w_sleet_l_d
				false -> R.drawable.w_sleet_l_n
				else -> R.drawable.w_sleet_l
			}
			1072, 1168 -> R.drawable.w_drizzle_l_f
			1087, 1273, 1276 -> when(isDay) {
				true -> R.drawable.w_thunder_d
				false -> R.drawable.w_thunder_n
				else -> R.drawable.w_thunder
			}
			1114 -> R.drawable.w_snow_b
			1117 -> R.drawable.w_blizzard
			1135 -> R.drawable.w_fog
			1147 -> R.drawable.w_fog_f
			1150, 1153 -> R.drawable.w_drizzle_l
			1171 -> R.drawable.w_drizzle_h_f
			1192, 1195, 1243 -> when(isDay) {
				true -> R.drawable.w_rain_h_d
				false -> R.drawable.w_rain_h_n
				else -> R.drawable.w_rain_h
			}
			1198, 1201 -> R.drawable.w_rain_f
			1222, 1225, 1258 -> when(isDay) {
				true -> R.drawable.w_snow_h_d
				false -> R.drawable.w_snow_h_n
				else -> R.drawable.w_snow_h
			}
			1237, 1261 -> when(isDay) {
				true -> R.drawable.w_ice_pellets_l_d
				false -> R.drawable.w_ice_pellets_l_n
				else -> R.drawable.w_ice_pellets_l
			}
			1246 -> if(isDay == false) R.drawable.w_rain_t_n else R.drawable.w_rain_t_d
			1252 -> if(isDay == false) R.drawable.w_sleet_h_n else R.drawable.w_sleet_h_d
			1264 -> if(isDay == false) R.drawable.w_ice_pellets_h_n else R.drawable.w_ice_pellets_h_d
			1279, 1282 -> when(isDay) {
				true -> R.drawable.w_snow_thunder_d
				false -> R.drawable.w_snow_thunder_n
				else -> R.drawable.w_snow_thunder
			}
			else -> throw IllegalArgumentException()
		}

		private fun meteorologicalToCircular(deg: Int) = (-deg + 90).mod(360)

		private fun localTimeToEpochSeconds(dayEpochSeconds: Int, time: String, zone: ZoneId): Int {
			return try {
				// stupid shits give startOfDaySeconds in utc and times in local
				val startOfDay = Instant.ofEpochSecond(dayEpochSeconds.toLong())
				val localTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("hh:mm a"))
				val zonedDt = ZonedDateTime.ofInstant(startOfDay, zone)
					.withHour(localTime.hour)
					.withMinute(localTime.minute)
				zonedDt.toEpochSecond().toInt()
			} catch(e: DateTimeParseException) {
				// when 'No sunrise/moonset/etc.' is sent
				e.printStackTrace()
				-1
			}
		}

		private fun moonPhaseIndex(phase: String) = when(phase) {
			"New Moon" -> 0
			"Waxing Crescent" -> 1
			"First Quarter" -> 2
			"Waxing Gibbous" -> 3
			"Full Moon" -> 4
			"Waning Gibbous" -> 5
			"Last Quarter" -> 6
			"Waning Crescent" -> 7
			else -> throw IllegalArgumentException("unrecognized moon phase")
		}

		// region todo unused
		private fun countryCodeToFlag(cc: String): String {
			val offset = 0x1F1E6 - 0x41 // tiny A - normal A
			val cps = IntArray(2) { i -> cc[i].code + offset }
			return String(cps, 0, cps.size)
		}

		private fun moonAge(instant: Instant): Int {
			return HijrahDate.from(instant.atZone(ZoneOffset.UTC))[ChronoField.DAY_OF_MONTH]
		}

		private fun moonAge2(instant: Instant): Float {
			// lunar cycle days
			val lunarCycle = 29.530588853f
			// a reference new moon at UTC
			val ref = LocalDateTime.of(2000, 1, 6, 18, 14)
			// dt at instant
			val dt = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
			// this loses a number of hours of accuracy
			val days = ChronoUnit.DAYS.between(ref, dt)
			val cycles = days / lunarCycle
			// take fractional part of cycles x full cycle = current lunation
			return (cycles % 1) * lunarCycle
		}
		// endregion
	}
}