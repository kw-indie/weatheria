package asceapps.weatheria.shared.data.repo

import asceapps.weatheria.shared.api.*
import asceapps.weatheria.shared.data.dao.WeatherInfoDao
import asceapps.weatheria.shared.data.entity.*
import asceapps.weatheria.shared.data.model.*
import asceapps.weatheria.shared.di.IoDispatcher
import asceapps.weatheria.shared.util.asResult
import asceapps.weatheria.shared.util.resultFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

@Singleton
class WeatherInfoRepo @Inject constructor(
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

	// todo need to return listable
	fun search(q: String? = null) = resultFlow<List<SearchResponse>> {
		withContext(ioDispatcher) {
			val query = if(q.isNullOrBlank()) {
				with(whoisApi.whois()) { "$lat,$lng" }
			} else q
			weatherApi.search(query)
		}
	}

	fun add(l: SearchResponse) = resultFlow<Unit> {
		val lastUpdate = (System.currentTimeMillis() / 1000).toInt()
		val pos = dao.getLocationsCount()
		val loc = with(l) {
			LocationEntity(id, lat, lng, name, country, zoneId, lastUpdate, pos)
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
		val c = extractCurrent(locationId, weatherApi.currentWeather(locationId))
		val h = extractHourly(locationId, weatherApi.hourlyForecast(locationId))
		val d = extractDaily(locationId, weatherApi.dailyForecast(locationId))
		Triple(c, h, d)
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

}

private fun extractCurrent(locationId: Int, resp: List<CurrentWeatherResponse>) =
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
			precip_mm,
			humidity,
			dewPoint_c.roundToInt(),
			clouds,
			vis_km,
			uv
		)
	}

private fun extractHourly(locationId: Int, resp: List<HourlyForecastResponse>) =
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
				0, // fixme remove
				precip_mm,
				humidity,
				dewPoint_c.roundToInt(),
				clouds,
				vis_km,
				pop,
				uv
			)
		}
	}

private fun extractDaily(locationId: Int, resp: DailyForecastResponse) =
	resp.forecasts.map { daily ->
		with(daily) {
			DailyEntity(
				locationId,
				dt,
				minTemp_c.roundToInt(),
				maxTemp_c.roundToInt(),
				day.condition, // fixme add night condition
				day.wind_kph, // fixme add night data
				day.precip_mm, // fixme add night data
				0, // fixme remove
				0f, // fixme remove
				max(day.pop, night.pop),
				uv,
				sunrise,
				sunrise,
				moonrise,
				moonset,
				moonPhaseIndex(moonAge.toFloat())
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
	} else { // todo also approximate from day if possible
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

private fun conditionIndex(condition: Int) = AccuWeatherApi.CONDITIONS.binarySearch(condition)

private fun conditionIconResId(condition: Int, isDay: Boolean? = null) = when(condition) {
	// todo
	1 -> 0 // sunny d
	2 -> 0 // mostly sunny d
	3 -> 0 // partly sunny d
	4 -> 0 // intermittent clouds d
	5 -> 0 // hazy sunshine d
	6 -> 0 // mostly cloudy d
	7 -> 0 // cloudy d/n
	8 -> 0 // overcast d/n
	11 -> 0 // fog d/n
	12 -> 0 // showers d/n
	13 -> 0 // mostly cloudy w/ showers d
	14 -> 0 // partly sunny w/ showers d
	15 -> 0 // t storms d/n
	16 -> 0 // mostly cloudy w/ t storms d
	17 -> 0 // partly sunny w/ t storms d
	18 -> 0 // rain d/n
	19 -> 0 // flurries d/n
	20 -> 0 // mostly cloudy w/ flurries d
	21 -> 0 // partly sunny w/ flurries d
	22 -> 0 // snow d/n
	23 -> 0 // mostly cloudy w/ snow d
	24 -> 0 // ice d/n
	25 -> 0 // sleet d/n
	26 -> 0 // freezing rain d/n
	29 -> 0 // rain n snow d/n
	30 -> 0 // hot d/n
	31 -> 0 // cold d/n
	32 -> 0 // windy d/n
	33 -> 0 // clear n
	34 -> 0 // mostly clear n
	35 -> 0 // partly cloudy n
	36 -> 0 // intermittent clouds n
	37 -> 0 // hazy moonshine n
	38 -> 0 // mostly cloudy n
	39 -> 0 // partly cloudy w/ showers n
	40 -> 0 // mostly cloudy w/ showers n
	41 -> 0 // partly cloudy w/ t storms n
	42 -> 0 // mostly cloudy w/ t storms n
	43 -> 0 // mostly cloudy w/ flurries n
	44 -> 0 // mostly cloudy w/ snow n
	else -> throw IllegalArgumentException()
}

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