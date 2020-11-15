package asceapps.weatheria.model

import asceapps.weatheria.db.WeatherConditionEntity
import asceapps.weatheria.db.WeatherInfoEntity
import java.time.Instant
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneOffset

object Mapper {

	fun entityToModel(conditions: Map<Int, WeatherConditionEntity>, info: WeatherInfoEntity): WeatherInfo {
		val zoneOffset = ZoneOffset.ofTotalSeconds(info.location.zoneOffset)
		val location = with(info.location) {
			Location(id, lat, lng, name, country, zoneOffset, pos)
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
				info.current.dt,
				condition.descEn,
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
			location, current, hourly, daily
		)
	}
}
