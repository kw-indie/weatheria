package asceapps.weatheria.api

import com.google.gson.annotations.SerializedName

/**
 * * all times are in UNIX seconds, UTC
 * * all temps are in kelvin
 *
 * @param timezone timezone name (major city/continent)
 * @param timezone_offset in seconds from UTC
 * @param current current weather
 * @param hourly forecast of 48 hours, starting this hour
 * @param daily forecast of 8 days, starting this hour (not midnight)
 * */
class OneCallResponse(
	val lat: Float,
	val lon: Float,
	val timezone: String,
	val timezone_offset: Int,
	val current: Current? = null,
	val hourly: List<Hourly>? = null,
	val daily: List<Daily>? = null
) {

	/**
	 * @param wind_speed wind_speed in m/s
	 * @param wind_deg wind_deg direction in degrees
	 * @param pressure pressure at sea level in hPa
	 * @param humidity humidity in % [0-100]
	 * @param dew_point dew_point temp at which dew forms
	 * @param clouds clouds cloudiness in % [0-100]
	 */
	abstract class BaseData(
		val dt: Int,
		val wind_speed: Float,
		val wind_deg: Int,
		val pressure: Int,
		val humidity: Int,
		val dew_point: Float,
		val clouds: Int,
		val weather: List<WeatherCondition>
	)

	/**
	 * @param dt current time
	 * @param visibility avg, in meters
	 */
	class Current(
		dt: Int,
		pressure: Int,
		humidity: Int,
		dew_point: Float,
		clouds: Int,
		wind_speed: Float,
		wind_deg: Int,
		weather: List<WeatherCondition>,
		val temp: Float,
		val feels_like: Float,
		val visibility: Int,
		val rain: Precipitation? = null,
		val snow: Precipitation? = null
	): BaseData(dt, wind_speed, wind_deg, pressure, humidity, dew_point, clouds, weather)

	/**
	 * @param dt beginning of forecast data hour
	 * @param pop probability of precipitation [0-1]
	 */
	class Hourly(
		dt: Int,
		pressure: Int,
		humidity: Int,
		dew_point: Float,
		clouds: Int,
		wind_speed: Float,
		wind_deg: Int,
		weather: List<WeatherCondition>,
		val temp: Float,
		val feels_like: Float,
		val visibility: Int,
		val pop: Float,
		val rain: Precipitation? = null,
		val snow: Precipitation? = null
	): BaseData(dt, wind_speed, wind_deg, pressure, humidity, dew_point, clouds, weather)

	/**
	 * @param dt beginning of forecast data day
	 * @param uvi midday uv index
	 * @param rain precipitation volume in mm
	 * @param snow same
	 */
	class Daily(
		dt: Int,
		pressure: Int,
		humidity: Int,
		dew_point: Float,
		clouds: Int,
		wind_speed: Float,
		wind_deg: Int,
		weather: List<WeatherCondition>,
		val sunrise: Int,
		val sunset: Int,
		val temp: Temp,
		val feels_like: Feel,
		val uvi: Float,
		val pop: Float,
		val rain: Float? = null,
		val snow: Float? = null
	): BaseData(dt, wind_speed, wind_deg, pressure, humidity, dew_point, clouds, weather) {

		class Temp(
			val morn: Float,
			val day: Float,
			val eve: Float,
			val night: Float,
			val min: Float,
			val max: Float
		)

		class Feel(val morn: Float, val day: Float, val eve: Float, val night: Float)
	}

	/**
	 * @param _1h precipitation volume in last hour, in mm
	 */
	class Precipitation(@SerializedName("1h") val _1h: Float)
}