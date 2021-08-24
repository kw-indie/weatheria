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
	@Flatten("forecast.forecastday") val forecastDays: List<ForecastDay>
) {

	/**
	 * @param country full country name, not just 2 letters
	 * @param zoneId timezone id, eg. Europe/London
	 */
	class Location(
		@SerializedName("lat") val lat: Float,
		@SerializedName("lon") val lng: Float,
		@SerializedName("name") val name: String,
		@SerializedName("country") val country: String,
		@SerializedName("tz_id") val zoneId: String
	)

	/**
	 * @param condition weather condition
	 * @param isDay 1 or 0, true or false
	 * @param wind_degree meteorological, aka CW and 0 is N
	 * @param pressure_mb in millibars
	 * @param humidity as percentage [0-100]
	 * @param clouds same as humidity
	 * @param vis_km visibility
	 * @param uv index: 1-2 Low, 3-5 Moderate, 6-7: High, 8-10: Very high, 11+: Extreme
	 */
	sealed class BaseData(
		@SerializedName("temp_c") val temp_c: Float,
		@SerializedName("feelslike_c") val feelsLike_c: Float,
		@Flatten("condition.code") val condition: Int,
		@SerializedName("is_day") val isDay: Int,
		@SerializedName("wind_kph") val wind_kph: Float,
		@SerializedName("wind_degree") val wind_degree: Int,
		@SerializedName("pressure_mb") val pressure_mb: Float,
		@SerializedName("precip_mm") val precip_mm: Float,
		@SerializedName("humidity") val humidity: Int,
		@SerializedName("cloud") val clouds: Int,
		@SerializedName("vis_km") val vis_km: Float,
		@SerializedName("uv") val uv: Float
	)

	/**
	 * @param dt unix seconds of data update on server
	 */
	class Current(
		@SerializedName("last_updated_epoch") val dt: Int,
		temp_c: Float,
		feelsLike_c: Float,
		condition: Int,
		isDay: Int,
		wind_kph: Float,
		wind_degree: Int,
		pressure_mb: Float,
		precip_mm: Float,
		humidity: Int,
		clouds: Int,
		vis_km: Float,
		uv: Float
	): BaseData(temp_c, feelsLike_c, condition, isDay, wind_kph, wind_degree, pressure_mb, precip_mm, humidity, clouds, vis_km, uv)

	/**
	 * @param dt dt at the start of this forecast day
	 * @param hourly hourly forecast (24)
	 * All astro times are in local format = hh:mm a
	 * @param moonPhase 8 phases
	 */
	class ForecastDay(
		@SerializedName("date_epoch") val dt: Int,
		@Flatten("day.condition.code") val condition: Int,
		@Flatten("astro.sunrise") val sunrise: String,
		@Flatten("astro.sunset") val sunset: String,
		@Flatten("astro.moonrise") val moonrise: String,
		@Flatten("astro.moonset") val moonset: String,
		@Flatten("astro.moon_phase") val moonPhase: String,
		@Flatten("day.mintemp_c") val minTemp_c: Float,
		@Flatten("day.maxtemp_c") val maxTemp_c: Float,
		@Flatten("day.maxwind_kph") val wind_kph: Float,
		@Flatten("day.totalprecip_mm") val precip_mm: Float,
		@Flatten("day.avghumidity") val humidity: Int,
		@Flatten("day.avgvis_km") val vis_km: Float,
		@Flatten("day.daily_chance_of_rain") val chanceOfRain: Int,
		@Flatten("day.daily_chance_of_snow") val chanceOfSnow: Int,
		@Flatten("day.uv") val uv: Float,
		@SerializedName("hour") val hourly: List<Hourly>
	)

	/**
	 * @param dt a random? past time < 1 day
	 * @param chanceOfRain as percentage (0-100)
	 */
	class Hourly(
		@SerializedName("time_epoch") val dt: Int,
		temp_c: Float,
		feelsLike_c: Float,
		condition: Int,
		isDay: Int,
		wind_kph: Float,
		wind_degree: Int,
		pressure_mb: Float,
		precip_mm: Float,
		humidity: Int,
		@SerializedName("dewpoint_c") val dewPoint_c: Float,
		clouds: Int,
		vis_km: Float,
		@SerializedName("chance_of_rain") val chanceOfRain: Int,
		@SerializedName("chance_of_snow") val chanceOfSnow: Int,
		uv: Float
	): BaseData(temp_c, feelsLike_c, condition, isDay, wind_kph, wind_degree, pressure_mb, precip_mm, humidity, clouds, vis_km, uv)
	 
}