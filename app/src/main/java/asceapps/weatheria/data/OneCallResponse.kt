package asceapps.weatheria.data

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * * all times are in UNIX seconds, UTC
 * * all temps are in kelvin
 *
 * @param timezone_offset in seconds from UTC
 * @param current current weather
 * @param hourly forecast of 48 hours, starting this hour
 * @param daily forecast of 8 days, starting next hour? (not midnight)
 * */
@Keep
class OneCallResponse(
	val timezone_offset: Int,
	val current: BaseData.Current,
	val hourly: List<BaseData.Hourly>,
	val daily: List<BaseData.Daily>
) {

	/**
	 * @param wind_speed in m/s
	 * @param wind_deg direction in degrees
	 * @param pressure at sea level in hPa
	 * @param humidity in % (0-100)
	 * @param dew_point temp at which dew forms
	 * @param clouds cloudiness in % (0-100)
	 * @param weather condition
	 */
	@Keep
	sealed class BaseData(
		val dt: Int,
		val wind_speed: Float,
		val wind_deg: Int,
		val pressure: Int,
		val humidity: Int,
		val dew_point: Float,
		val clouds: Int,
		val weather: List<Weather>
	) {

		/**
		 * @param dt time when this data was collected
		 * @param visibility avg, in meters
		 */
		@Keep
		class Current(
			dt: Int,
			pressure: Int,
			humidity: Int,
			dew_point: Float,
			clouds: Int,
			wind_speed: Float,
			wind_deg: Int,
			weather: List<Weather>,
			val temp: Float,
			val feels_like: Float,
			val visibility: Int,
			val rain: Precipitation? = null,
			val snow: Precipitation? = null
		): BaseData(dt, wind_speed, wind_deg, pressure, humidity, dew_point, clouds, weather)

		/**
		 * @param dt beginning of forecast data hour
		 * @param pop probability of precipitation (0-1)
		 */
		@Keep
		class Hourly(
			dt: Int,
			pressure: Int,
			humidity: Int,
			dew_point: Float,
			clouds: Int,
			wind_speed: Float,
			wind_deg: Int,
			weather: List<Weather>,
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
		@Keep
		class Daily(
			dt: Int,
			pressure: Int,
			humidity: Int,
			dew_point: Float,
			clouds: Int,
			wind_speed: Float,
			wind_deg: Int,
			weather: List<Weather>,
			val sunrise: Int,
			val sunset: Int,
			val temp: Temp,
			val feels_like: Feel,
			val uvi: Float,
			val pop: Float,
			val rain: Float? = null,
			val snow: Float? = null
		): BaseData(dt, wind_speed, wind_deg, pressure, humidity, dew_point, clouds, weather) {

			@Keep
			class Temp(
				val morn: Float,
				val day: Float,
				val eve: Float,
				val night: Float,
				val min: Float,
				val max: Float
			)

			@Keep
			class Feel(val morn: Float, val day: Float, val eve: Float, val night: Float)
		}
	}

	/**
	 * @param id condition id
	 */
	@Keep
	class Weather(val id: Int)

	/**
	 * @param _1h precipitation volume in last hour, in mm
	 */
	@Keep
	class Precipitation(@SerializedName("1h") val _1h: Float)
}