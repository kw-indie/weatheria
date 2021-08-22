package asceapps.weatheria.data.api

import asceapps.weatheria.data.base.BaseLocation
import asceapps.weatheria.data.base.Listable
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

	companion object {
		const val BASE_URL = "https://api.weatherapi.com/v1/"
		const val KEY_PARAM = "key"

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
	@SerializedName("id") override val id: Int,
	@SerializedName("lat") override val lat: Float,
	@SerializedName("lon") override val lng: Float,
	@SerializedName("name") override val name: String,
	@SerializedName("country") override val country: String
): BaseLocation, Listable {

	override val hash get() = id
}

class ForecastResponse(
	@SerializedName("location") val location: Location,
	@SerializedName("current") val current: Current,
	@SerializedName("forecast") val forecast: Forecast
) {

	/**
	 * @param country full country name, not just 2 letters
	 * @param zoneId timezone id, eg. Europe/London
	 */
	class Location(
		@SerializedName("lat") val lat: Float,
		@SerializedName("lon") val lon: Float,
		@SerializedName("name") val name: String,
		@SerializedName("country") val country: String,
		@SerializedName("tz_id") val zoneId: String
	)

	/**
	 * @param lastUpdate unix seconds of data update on server
	 * @param condition weather condition
	 * @param isDay 1 or 0, true or false
	 * @param pressure_mb in millibars
	 * @param humidity as percentage [0-100]
	 * @param cloud same as humidity
	 * @param vis_km visibility
	 * @param uv index: 1-2 Low, 3-5 Moderate, 6-7: High, 8-10: Very high, 11+: Extreme
	 */
	class Current(
		@SerializedName("last_updated_epoch") val lastUpdate: Int,
		@SerializedName("temp_c") val temp_c: Float,
		@SerializedName("feelslike_c") val feelslike_c: Float,
		@SerializedName("condition") val condition: Condition,
		@SerializedName("is_day") val isDay: Int, // 1 or 0
		@SerializedName("wind_kph") val wind_kph: Float,
		@SerializedName("wind_degree") val wind_degree: Int,
		@SerializedName("pressure_mb") val pressure_mb: Float,
		@SerializedName("precip_mm") val precip_mm: Float,
		@SerializedName("humidity") val humidity: Int,
		@SerializedName("cloud") val cloud: Int,
		@SerializedName("vis_km") val vis_km: Float,
		@SerializedName("uv") val uv: Float
	)

	class Condition(@SerializedName("code") val code: Int)

	class Forecast(@SerializedName("forecastday") val forecastday: List<ForecastDay>)

	/**
	 * @param dt dt at the start of this forecast day
	 * @param hourly hourly forecast (24)
	 * @param day day info
	 * @param astro astro info
	 */
	class ForecastDay(
		@SerializedName("date_epoch") val dt: Int,
		@SerializedName("hour") val hourly: List<Hourly>,
		@SerializedName("day") val day: Day,
		@SerializedName("astro") val astro: Astro
	)

	/**
	 * @param dt a random? past time < 1 day
	 * @param chance_of_rain as percentage (0-100)
	 */
	class Hourly(
		@SerializedName("time_epoch") val dt: Int,
		@SerializedName("temp_c") val temp_c: Float,
		@SerializedName("feelslike_c") val feelslike_c: Float,
		@SerializedName("condition") val condition: Condition,
		@SerializedName("is_day") val isDay: Int,
		@SerializedName("wind_kph") val wind_kph: Float,
		@SerializedName("wind_degree") val wind_degree: Int,
		@SerializedName("pressure_mb") val pressure_mb: Float,
		@SerializedName("precip_mm") val precip_mm: Float,
		@SerializedName("humidity") val humidity: Int,
		@SerializedName("dewpoint_c") val dewpoint_c: Float,
		@SerializedName("cloud") val cloud: Int,
		@SerializedName("vis_km") val vis_km: Float,
		@SerializedName("chance_of_rain") val chance_of_rain: Int,
		@SerializedName("chance_of_snow") val chance_of_snow: Int,
		@SerializedName("uv") val uv: Float
	)

	class Day(
		@SerializedName("mintemp_c") val mintemp_c: Float,
		@SerializedName("maxtemp_c") val maxtemp_c: Float,
		@SerializedName("condition") val condition: Condition,
		@SerializedName("maxwind_kph") val maxwind_kph: Float,
		@SerializedName("totalprecip_mm") val totalprecip_mm: Float,
		@SerializedName("avghumidity") val avghumidity: Int,
		@SerializedName("avgvis_km") val avgvis_km: Float,
		@SerializedName("daily_chance_of_rain") val daily_chance_of_rain: Int,
		@SerializedName("daily_chance_of_snow") val daily_chance_of_snow: Int,
		@SerializedName("uv") val uv: Float,
	)

	/**
	 * All times are in local format = hh:mm a
	 * @param moon_phase 8 phases
	 */
	class Astro(
		@SerializedName("sunrise") val sunrise: String,
		@SerializedName("sunset") val sunset: String,
		@SerializedName("moonrise") val moonrise: String,
		@SerializedName("moonset") val moonset: String,
		@SerializedName("moon_phase") val moon_phase: String
	)
}