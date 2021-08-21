package asceapps.weatheria.data.api

import asceapps.weatheria.BuildConfig
import asceapps.weatheria.data.base.BaseLocation
import asceapps.weatheria.data.base.Listable
import com.google.gson.annotations.SerializedName
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

	companion object {

		fun create(): WeatherApi = buildService(
			"https://api.weatherapi.com/v1/",
			"key", BuildConfig.WEATHER_API_KEY
		).create()

		const val AUTO_IP = "auto:ip"
		val CONDITIONS = intArrayOf(
			1000, 1003, 1006, 1009, 1030, 1063, 1066, 1069, 1072, 1087, 1114, 1117, 1135, 1147, 1150, 1153,
			1168, 1171, 1180, 1183, 1186, 1189, 1192, 1195, 1198, 1201, 1204, 1207, 1210, 1213, 1216, 1219,
			1222, 1225, 1237, 1240, 1243, 1246, 1249, 1252, 1255, 1258, 1261, 1264, 1273, 1276, 1279, 1282
		)
	}

	/**
	 * @param q latLng, name, US zip, UK postcode, [AUTO_IP], ip address, or others
	 */
	@GET("search.json")
	suspend fun search(@Query("q") q: String): List<SearchResponse>

	/**
	 * @param q see [search] for possible values
	 * @param days 1-10. free account gets only 3 max
	 * We can specify unixdt unix seconds at which the forecast data starts (within next 10 days only).
	 * but it wil return only 1 forecast day.
	 */
	@GET("forecast.json")
	suspend fun forecast(@Query("q") q: String, @Query("days") days: Int): ForecastResponse
}

class SearchResponse(
	override val id: Int,
	override val lat: Float,
	@SerializedName("lon") override val lng: Float,
	override val name: String,
	override val country: String
): BaseLocation, Listable {

	override val hash get() = id
}

class ForecastResponse(
	val location: Location,
	val current: Current,
	val forecast: Forecast
) {

	/**
	 * @param country full country name, not just 2 letters
	 * @param tz_id timezone id, eg. Europe/London
	 * @param localtime_epoch seconds since epoch
	 */
	class Location(
		val lat: Float,
		val lon: Float,
		val name: String,
		val country: String,
		val tz_id: String,
		val localtime_epoch: Int
	)

	/**
	 * @param last_updated_epoch unix seconds of data update on server
	 * @param condition weather condition
	 * @param is_day 1 or 0, true or false
	 * @param pressure_mb in millibars
	 * @param humidity as percentage [0-100]
	 * @param cloud same as humidity
	 * @param vis_km visibility
	 * @param uv index: 1-2 Low, 3-5 Moderate, 6-7: High, 8-10: Very high, 11+: Extreme
	 */
	class Current(
		val last_updated_epoch: Int,
		val temp_c: Float,
		val feelslike_c: Float,
		val condition: Condition,
		val is_day: Int, // 1 or 0
		val wind_kph: Float,
		val wind_degree: Int,
		val pressure_mb: Float,
		val precip_mm: Float,
		val humidity: Int,
		val cloud: Int,
		val vis_km: Float,
		val uv: Float
	)

	class Condition(val code: Int)

	class Forecast(val forecastday: List<ForecastDay>)

	/**
	 * @param date_epoch dt at the start of this forecast day
	 * @param hour hourly forecast (24)
	 * @param day day info
	 * @param astro astro info
	 */
	class ForecastDay(
		val date_epoch: Int,
		val hour: List<Hour>,
		val day: Day,
		val astro: Astro
	)

	/**
	 * @param time_epoch a random? past time < 1 day
	 * @param chance_of_rain as percentage (0-100)
	 */
	class Hour(
		val time_epoch: Int,
		val temp_c: Float,
		val feelslike_c: Float,
		val condition: Condition,
		val is_day: Int,
		val wind_kph: Float,
		val wind_degree: Int,
		val pressure_mb: Float,
		val precip_mm: Float,
		val humidity: Int,
		val dewpoint_c: Float,
		val cloud: Int,
		val vis_km: Float,
		val chance_of_rain: Int,
		val chance_of_snow: Int,
		val uv: Float
	)

	class Day(
		val mintemp_c: Float,
		val maxtemp_c: Float,
		val condition: Condition,
		val maxwind_kph: Float,
		val totalprecip_mm: Float,
		val avghumidity: Int,
		val avgvis_km: Float,
		val daily_chance_of_rain: Int,
		val daily_chance_of_snow: Int,
		val uv: Float,
	)

	/**
	 * All times are in local format = hh:mm a
	 * @param moon_phase 8 phases
	 */
	class Astro(
		val sunrise: String,
		val sunset: String,
		val moonrise: String,
		val moonset: String,
		val moon_phase: String
	)
}