package asceapps.weatheria.model

import asceapps.weatheria.api.FindResponse
import asceapps.weatheria.api.OneCallResponse
import asceapps.weatheria.api.WeatherCondition
import asceapps.weatheria.db.CurrentEntity
import asceapps.weatheria.db.DailyEntity
import asceapps.weatheria.db.HourlyEntity
import asceapps.weatheria.db.LocationEntity
import asceapps.weatheria.db.WeatherConditionEntity
import asceapps.weatheria.db.WeatherInfoEntity
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.roundToInt

object Mapper {

	fun map(from: FindResponse): List<FoundLocation> {
		return from.list.map {
			val coord = it.coord
			val weather = it.weather[0]
			FoundLocation(
				it.id,
				coord.lat,
				coord.lon,
				it.name,
				it.sys.country,
				it.main.temp.roundToInt(),
				weather.main,
				weather.icon
			)
		}
	}

	fun map(foundLocation: FoundLocation, resp: OneCallResponse):
		Pair<Map<Int, WeatherConditionEntity>, WeatherInfoEntity> {
		val conditions = HashMap<Int, WeatherConditionEntity>()
		val location = with(foundLocation) {
			LocationEntity(id, lat, lng, name, country, resp.timezone_offset)
		}
		// if we are parsing with a found location, all nullables are non-null
		val current = extractCurrent(location.id, resp.current!!, conditions)
		val hourly = extractHourly(location.id, resp.hourly!!, conditions)
		val daily = extractDaily(location.id, resp.daily!!, conditions)
		return Pair(
			conditions,
			WeatherInfoEntity(location, current, hourly, daily)
		)
	}

	fun extractCurrent(locationId: Int, resp: OneCallResponse.Current,
		conditions: MutableMap<Int, WeatherConditionEntity>): CurrentEntity {
		return with(resp) {
			val condition = map(weather[0])
			conditions.putIfAbsent(condition.id, condition)
			CurrentEntity(
				locationId,
				dt,
				condition.id,
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
	}

	fun extractHourly(locationId: Int, respList: List<OneCallResponse.Hourly>,
		conditions: MutableMap<Int, WeatherConditionEntity>): List<HourlyEntity> {
		return respList.map {
			with(it) {
				val weather = weather[0]
				conditions.putIfAbsent(weather.id, map(weather))
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
	}

	fun extractDaily(locationId: Int, respList: List<OneCallResponse.Daily>,
		conditions: MutableMap<Int, WeatherConditionEntity>): List<DailyEntity> {
		return respList.map {
			with(it) {
				val weather = weather[0]
				val temp = it.temp
				val feel = it.feels_like
				conditions.putIfAbsent(weather.id, map(weather))
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

	fun extractConditionIds(info: WeatherInfoEntity): List<Int> {
		return with(info) {
			mutableListOf(current.conditionId) +
				hourly.map {it.conditionId} +
				daily.map {it.conditionId}
		}
	}

	fun map(conditions: Map<Int, WeatherConditionEntity>, info: WeatherInfoEntity): WeatherInfo {
		val zoneOffset = ZoneOffset.ofTotalSeconds(info.location.zoneOffset)
		val location = with(info.location) {
			Location(id, lat, lng, name, country, zoneOffset)
		}
		val daily = info.daily.map {
			with(it) {
				Daily(
					OffsetDateTime.ofInstant(Instant.ofEpochSecond(dt.toLong()), zoneOffset).toLocalDate(),
					OffsetTime.ofInstant(Instant.ofEpochSecond(sunrise.toLong()), zoneOffset).toLocalTime(),
					OffsetTime.ofInstant(Instant.ofEpochSecond(sunset.toLong()), zoneOffset).toLocalTime(),
					conditions.getValue(conditionId).icon,
					minTemp,
					maxTemp,
					pop,
					uvi
				)
			}
		}
		val hourly = info.hourly.map {
			with(it) {
				Hourly(
					OffsetTime.ofInstant(Instant.ofEpochSecond(dt.toLong()), zoneOffset).toLocalTime(),
					conditions.getValue(conditionId).icon,
					temp,
					pop
				)
			}
		}
		val current = with(info.current) {
			val condition = conditions.getValue(conditionId)
			Current(
				condition.main,
				condition.desc,
				condition.icon,
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
			LocalDateTime.ofInstant(
				Instant.ofEpochSecond(info.current.dt.toLong()),
				ZoneId.systemDefault()
			)
		)
	}

	private fun map(from: WeatherCondition) = with(from) {
		WeatherConditionEntity(id, main, description, icon)
	}
}
