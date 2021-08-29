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
				sunrise,
				moonrise,
				moonset,
				moonPhaseIndex(moonAge.toFloat())
			)
		}
	}

private fun entityToModel(info: WeatherInfoEntity): WeatherInfo {
	val l = with(info.location) {
		Location(id, lat, lng, name, country, ZoneId.of(zoneId))
	}
	val d = info.daily.map {
		with(it) {
			Daily(
				toInstant(dt),
				Temp(minTemp),
				Temp(maxTemp),
				conditionIconResId(dayCondition),
				conditionIconResId(nightCondition),
				Percent(max(dayPop, nightPop)),
				UV(uv),
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
				conditionIconResId(condition, isDay),
				Temp(temp),
				Percent(pop)
			)
		}
	}
	val nowSeconds = currentSeconds()
	val elapsedHours = nowSeconds - info.location.lastUpdate / 3600
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
					conditionIconResId(condition, isDay),
					Temp(temp),
					Temp(feelsLike),
					Distance(windSpeed),
					windDir,
					Pressure(pressure),
					Percent(humidity),
					Temp(dewPoint),
					Percent(clouds),
					Distance(visibility),
					UV(uv)
				)
			}
		}
		ACCURACY_HIGH -> { // approximate
			val thisHourEntity = thisHourEntity(info.hourly) ?: info.hourly.last()
			thisHourEntity.run {
				Current(
					conditionIndex(condition),
					conditionIconResId(condition, isDay),
					Temp(temp),
					Temp(feelsLike),
					Distance(windSpeed),
					windDir,
					Pressure(UNKNOWN_INT),
					Percent(humidity),
					Temp(dewPoint),
					Percent(clouds),
					Distance(visibility),
					UV(uv)
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
					conditionIconResId(dayCondition, isDay), // fixme
					if(isDay) Temp(maxTemp) else Temp(minTemp),  // too simple approximation
					Temp(UNKNOWN_INT),
					if(isDay) Distance(dayWindSpeed) else Distance(nightWindSpeed),
					UNKNOWN_INT,
					Pressure(UNKNOWN_INT),
					Percent(UNKNOWN_INT),
					Temp(UNKNOWN_INT),
					if(isDay) Percent(dayClouds) else Percent(nightClouds),
					Distance(UNKNOWN_FLT),
					UV(uv)
				)
			}
		}
	}
	return WeatherInfo(l, c, h, d, toInstant(info.location.lastUpdate), accuracy, info.location.pos)
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