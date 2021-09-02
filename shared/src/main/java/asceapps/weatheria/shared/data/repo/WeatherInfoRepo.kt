package asceapps.weatheria.shared.data.repo

import asceapps.weatheria.shared.api.*
import asceapps.weatheria.shared.data.dao.WeatherInfoDao
import asceapps.weatheria.shared.data.entity.*
import asceapps.weatheria.shared.data.model.*
import asceapps.weatheria.shared.di.IoDispatcher
import asceapps.weatheria.shared.ext.asResult
import asceapps.weatheria.shared.ext.resultFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

@Singleton
class WeatherInfoRepo @Inject internal constructor(
	private val weatherApi: AccuWeatherApi,
	private val whoisApi: IPWhoisApi,
	private val dao: WeatherInfoDao,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

	// todo optimize these so they only return needed data, especially
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

	fun search(q: String? = null) = resultFlow<List<Location>> {
		withContext(ioDispatcher) {
			val query = if(q.isNullOrBlank()) {
				with(whoisApi.whois()) { "$lat,$lng" }
			} else q
			val resp = weatherApi.search(query)
			resp.map {
				with(it) {
					Location(id, lat, lng, name, country, ZoneId.of(zoneId))
				}
			}
		}
	}

	fun add(l: Location) = resultFlow<Unit> {
		val lastUpdate = (System.currentTimeMillis() / 1000).toInt()
		val pos = dao.getLocationsCount()
		val loc = with(l) {
			LocationEntity(id, lat, lng, name, country, zoneId.id, lastUpdate, pos)
		}
		val (c, h, d) = fetchForecast(l.id)
		dao.insert(loc, c, h, d)
	}

	fun refresh(info: WeatherInfo) = resultFlow<Unit> {
		internalRefresh(info.id)
	}

	fun refreshAll() = resultFlow<Unit> {
		val ids = dao.getLocationIds()
		ids.forEachIndexed { i, id ->
			internalRefresh(id)
			emit(Loading(100 / ids.size * (i + 1)))
		}
	}

	private suspend fun internalRefresh(locationId: Int) {
		// todo make this call worker with ids as workData
		val (c, h, d) = fetchForecast(locationId)
		// keep this value in sync with 'add()'
		val lastUpdate = (System.currentTimeMillis() / 1000).toInt()
		dao.update(lastUpdate, c, h, d)
	}

	private suspend fun fetchForecast(locationId: Int) = withContext(ioDispatcher) {
		val c = extractCurrentEntity(locationId, weatherApi.currentWeather(locationId))
		val h = extractHourlyEntity(locationId, weatherApi.hourlyForecast(locationId))
		val d = extractDailyEntity(locationId, weatherApi.dailyForecast(locationId))
		Triple(c, h, d)
	}

	suspend fun reorder(info: WeatherInfo, toPos: Int) {
		with(info.location) { dao.reorder(id, info.pos, toPos) }
	}

	suspend fun delete(info: WeatherInfo) {
		with(info.location) { dao.delete(id, info.pos) }
	}

	suspend fun deleteAll() {
		dao.deleteAll()
	}

	private fun entityToModel(info: WeatherInfoEntity): WeatherInfo {
		val l = with(info.location) {
			Location(id, lat, lng, name, country, ZoneId.of(zoneId))
		}
		val d = info.daily.map {
			with(it) {
				Daily(
					toInstant(dt),
					minTemp,
					maxTemp,
					dayCondition,
					nightCondition,
					max(dayPop, nightPop),
					uv,
					toInstant(sunrise),
					toInstant(sunset),
					toInstant(moonrise),
					toInstant(moonset),
					moonPhaseIndex
				)
			}
		}
		val h = info.hourly.map {
			with(it) {
				Hourly(
					toInstant(dt),
					condition,
					temp,
					pop
				)
			}
		}
		val nowSeconds = currentSeconds()
		val elapsedHours = (nowSeconds - info.location.lastUpdate) / 3600
		val accuracy = when {
			elapsedHours < 1 -> ACCURACY_FRESH
			elapsedHours < info.hourly.size -> ACCURACY_HIGH
			elapsedHours < info.daily.size * 24 -> ACCURACY_LOW
			else -> ACCURACY_OUTDATED // no data to approximate from
		}
		val c = when(accuracy) {
			ACCURACY_FRESH -> {
				with(info.current) {
					Current(
						conditionIndex(condition),
						condition,
						temp,
						feelsLike,
						windSpeed,
						meteorologicalToCircular(windDir),
						pressure,
						humidity,
						dewPoint,
						clouds,
						visibility,
						uv
					)
				}
			}
			ACCURACY_HIGH -> { // approximate
				val thisHourEntity = thisHourEntity(info.hourly) ?: info.hourly.last()
				thisHourEntity.run {
					Current(
						conditionIndex(condition),
						condition,
						temp,
						feelsLike,
						windSpeed,
						meteorologicalToCircular(windDir),
						UNKNOWN_INT,
						humidity,
						dewPoint,
						clouds,
						visibility,
						uv
					)
				}
			}
			else -> { // ACCURACY_MEDIUM, ACCURACY_LOW, ACCURACY_OUTDATE
				// it's prolly better and less work to show old data from last update
				// than have null when data is outdated
				val todayEntity = todayEntity(info.daily) ?: info.daily.last()
				todayEntity.run {
					val isDay = nowSeconds in sunrise..sunset
					Current(
						conditionIndex(if(isDay) dayCondition else nightCondition),
						if(isDay) dayCondition else nightCondition,
						if(isDay) maxTemp else minTemp,  // too simple approximation
						UNKNOWN_INT,
						if(isDay) dayWindSpeed else nightWindSpeed,
						UNKNOWN_INT,
						UNKNOWN_INT,
						UNKNOWN_INT,
						UNKNOWN_INT,
						if(isDay) dayClouds else nightClouds,
						UNKNOWN_FLT,
						uv
					)
				}
			}
		}
		return WeatherInfo(l, c, h, d, toInstant(info.location.lastUpdate), accuracy, info.location.pos)
	}
}

private fun extractCurrentEntity(locationId: Int, resp: List<CurrentWeatherResponse>) =
	with(resp[0]) {
		CurrentEntity(
			locationId,
			lastUpdate,
			temp_c.roundToInt(),
			feelsLike_c.roundToInt(),
			condition,
			isDay,
			wind_kph,
			wind_degree,
			pressure_mb,
			humidity,
			dewPoint_c.roundToInt(),
			clouds,
			vis_km,
			uv
		)
	}

private fun extractHourlyEntity(locationId: Int, resp: List<HourlyForecastResponse>) =
	resp.map { hourly ->
		with(hourly) {
			HourlyEntity(
				locationId,
				dt,
				temp_c.roundToInt(),
				feelsLike_c.roundToInt(),
				condition,
				isDay,
				wind_kph,
				wind_degrees,
				humidity,
				dewPoint_c.roundToInt(),
				clouds,
				vis_km,
				pop,
				uv
			)
		}
	}

private fun extractDailyEntity(locationId: Int, resp: DailyForecastResponse) =
	resp.forecasts.map { daily ->
		with(daily) {
			DailyEntity(
				locationId,
				dt,
				minTemp_c.roundToInt(),
				maxTemp_c.roundToInt(),
				day.condition,
				night.condition,
				day.wind_kph,
				night.wind_kph,
				day.pop,
				night.pop,
				day.clouds,
				night.clouds,
				uv,
				sunrise,
				sunset,
				moonrise,
				moonset,
				moonPhaseIndex(moonAge.toFloat())
			)
		}
	}

private fun toInstant(epochSeconds: Int) = Instant.ofEpochSecond(epochSeconds.toLong())

private fun conditionIndex(condition: Int) = AccuWeatherApi.CONDITIONS.binarySearch(condition)

private fun meteorologicalToCircular(deg: Int) = (-deg + 90).mod(360)

private fun moonPhaseIndex(age: Float) = when(age) {
	in 1f..6.38f -> 1
	in 6.38f..8.38f -> 2
	in 8.38f..13.77f -> 3
	in 13.77f..15.77f -> 4
	in 15.77f..21.15f -> 5
	in 21.15f..23.15f -> 6
	in 23.15f..28.5f -> 7
	else -> 0
}

// todo use in search results?
private fun countryCodeToFlag(cc: String): String {
	val offset = 0x1F1E6 - 0x41 // tiny A - normal A
	val cps = IntArray(2) { i -> cc[i].code + offset }
	return String(cps, 0, cps.size)
}