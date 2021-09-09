package asceapps.weatheria.shared.data.repo

import asceapps.weatheria.shared.api.*
import asceapps.weatheria.shared.data.dao.WeatherInfoDao
import asceapps.weatheria.shared.data.entity.*
import asceapps.weatheria.shared.data.model.*
import asceapps.weatheria.shared.data.result.KnownError
import asceapps.weatheria.shared.data.result.Loading
import asceapps.weatheria.shared.data.util.*
import asceapps.weatheria.shared.di.IoDispatcher
import asceapps.weatheria.shared.ext.asResultFlow
import asceapps.weatheria.shared.ext.resultFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.InterruptedIOException
import java.net.UnknownHostException
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

	private val netErrTransformer: (Throwable) -> Throwable = {
		when(it) {
			is InterruptedIOException -> KnownError(ERROR_TIMED_OUT)
			is UnknownHostException -> KnownError(ERROR_NO_INTERNET)
			is HttpException -> KnownError(if(it.code() == 503) ERROR_SERVER_DAILY_QUOTA else ERROR_SERVER)
			else -> it
		}
	}

	// todo optimize these so they only return needed data, especially
	fun getAll() = dao.loadAll()
		.map { list -> list.map { e -> entityToModel(e) } }
		.asResultFlow() // concerned over 2 consecutive Flow.map()'s
		.flowOn(ioDispatcher)

	fun get(locationId: Int) = dao.get(locationId)
		.map { e -> entityToModel(e) }
		.asResultFlow()
		.flowOn(ioDispatcher)

	fun getByPos(pos: Int) = dao.getByPos(pos)
		.map { e -> entityToModel(e) }
		.asResultFlow()
		.flowOn(ioDispatcher)

	fun search(q: String? = null) = resultFlow<List<Location>>(netErrTransformer) {
		withContext(ioDispatcher) {
			val query = if(q.isNullOrBlank()) {
				with(whoisApi.whois()) { "$lat,$lng" }
			} else q
			val resp = weatherApi.search(query)
			resp.map {
				// there are special locations where the id is a mix of digits and letters, eg. 1-299427_1_AL
				val itsId = it.id
				val parsedId = itsId.toIntOrNull()
					?: itsId.substring(itsId.indexOf('-') + 1, itsId.indexOf('_')).toInt()
				with(it) {
					Location(parsedId, lat, lng, name, country, cc, ZoneId.of(zoneId))
				}
			}
		}
	}

	fun add(l: Location) = resultFlow<Unit>(netErrTransformer) {
		val lastUpdate = (System.currentTimeMillis() / 1000).toInt()
		val pos = dao.getLocationsCount()
		val loc = with(l) {
			LocationEntity(id, lat, lng, name, country, cc, zoneId.id, lastUpdate, pos)
		}
		val (c, h, d) = fetchForecast(l.id)
		dao.insert(loc, c, h, d)
	}

	fun refresh(info: WeatherInfo) = resultFlow<Unit>(netErrTransformer) {
		internalRefresh(info.id)
	}

	fun refreshAll() = resultFlow<Unit>(netErrTransformer) {
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
				dayCondition,
				nightCondition,
				dayWind_kph,
				nightWind_kph,
				dayWind_degrees,
				nightWind_degrees,
				dayPop,
				nightPop,
				dayClouds,
				nightClouds,
				uv,
				sunrise,
				sunset,
				moonrise,
				moonset,
				moonAge
			)
		}
	}

private fun entityToModel(info: WeatherInfoEntity): WeatherInfo {
	val l = with(info.location) {
		Location(id, lat, lng, name, country, cc, ZoneId.of(zoneId))
	}
	val d = info.daily.map {
		with(it) {
			Daily(
				toInstant(dt),
				minTemp,
				maxTemp,
				conditionIndex(dayCondition),
				conditionIndex(nightCondition),
				max(dayPop, nightPop),
				toInstant(sunrise),
				toInstant(sunset),
				toInstant(moonrise),
				toInstant(moonset),
				moonAge
			)
		}
	}
	val h = info.hourly.map {
		with(it) {
			Hourly(
				toInstant(dt),
				conditionIndex(condition),
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
		else -> { // ACCURACY_MEDIUM, ACCURACY_LOW, ACCURACY_OUTDATED
			// it's prolly better and less work to show old data from last update
			// than have null when data is outdated
			val todayEntity = todayEntity(info.daily) ?: info.daily.last()
			todayEntity.run {
				val isDay = nowSeconds in sunrise..sunset
				Current(
					conditionIndex(if(isDay) dayCondition else nightCondition),
					if(isDay) maxTemp else minTemp,  // too simple approximation
					UNKNOWN_INT,
					if(isDay) dayWindSpeed else nightWindSpeed,
					if(isDay) dayWindDir else nightWindDir,
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

private fun toInstant(epochSeconds: Int) = Instant.ofEpochSecond(epochSeconds.toLong())

private fun conditionIndex(condition: Int) = AccuWeatherApi.CONDITIONS.binarySearch(condition)

private fun meteorologicalToCircular(deg: Int) = (-deg + 90).mod(360)