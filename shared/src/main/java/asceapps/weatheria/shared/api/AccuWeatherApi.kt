package asceapps.weatheria.shared.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface AccuWeatherApi {

	companion object {
		const val BASE_URL = "https://dataservice.accuweather.com"
		const val KEY_PARAM = "apikey"

		// 40 conditions
		val CONDITIONS = intArrayOf(
			1, 2, 3, 4, 5, 6, 7, 8, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
			24, 25, 26, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44
		)
	}

	// possible 'language' param for all these requests

	/**
	 * @param q location name (optionally separated by country code), zipcode, coordinates or even IP address.
	 */
	@GET("locations/v1/search")
	suspend fun search(@Query("q") q: String): List<SearchResponse>

	/**
	 * @param id a location's unique id.
	 *
	 * Defaults to no details, but we modify the url to do otherwise.
	 * This retarded api returns a single item array for this request.
	 */
	@GET("currentconditions/v1/{id}?details=true")
	suspend fun currentWeather(@Path("id") id: Int): List<CurrentWeatherResponse>

	/**
	 * @param id a location's unique id.
	 *
	 * Max of 12 hours for free
	 * Defaults to no details and imperial units, but we modify the url to do otherwise.
	 */
	@GET("forecasts/v1/hourly/12hour/{id}?details=true&metric=true")
	suspend fun hourlyForecast(@Path("id") id: Int): List<HourlyForecastResponse>

	/**
	 * @param id a location's unique id
	 *
	 * Max of 5 days for free
	 * Default to no details and imperial units, but we modify the url to do otherwise.
	 */
	@GET("forecasts/v1/daily/5day/{id}?details=true&metric=true")
	suspend fun dailyForecast(@Path("id") id: Int): DailyForecastResponse
}

/**
 * @param cc 2-letter country code
 */
internal class SearchResponse(
	@SerializedName("Key") val id: String,
	@Flatten("GeoPosition.Latitude") val lat: Float,
	@Flatten("GeoPosition.Longitude") val lng: Float,
	@SerializedName("LocalizedName") val name: String,
	@Flatten("Country.LocalizedName") val country: String,
	@Flatten("Country.ID") val cc: String,
	// @Flatten("Country.Name") override val country: String, todo use this and rename above to cc
	@Flatten("TimeZone.Name") val zoneId: String
)

/**
 * According to accuWeather, uv levels go like this:
 * * 0.0-2.5 = low
 * * 2.5-5.5 = med
 * * 5.5-7.5 = high
 * * 7.5-10.5 = v high
 *
 * @param wind_degree mathematical, aka CCW and 0 is E
 */
internal class CurrentWeatherResponse(
	@SerializedName("EpochTime") val lastUpdate: Int,
	@Flatten("Temperature.Metric.Value") val temp_c: Float,
	@Flatten("RealFeelTemperature.Metric.Value") val feelsLike_c: Float,
	@SerializedName("WeatherIcon") val condition: Int,
	@SerializedName("IsDayTime") val isDay: Boolean,
	@Flatten("Wind.Speed.Metric.Value") val wind_kph: Float,
	@Flatten("Wind.Direction.Degrees") val wind_degree: Int,
	@Flatten("Pressure.Metric.Value") val pressure_mb: Int,
	@Flatten("Precip1hr.Metric.Value") val precip_mm: Int,
	@SerializedName("RelativeHumidity") val humidity: Int,
	@Flatten("DewPoint.Metric.Value") val dewPoint_c: Float,
	@SerializedName("CloudCover") val clouds: Int,
	@Flatten("Visibility.Metric.Value") val vis_km: Float,
	@SerializedName("UVIndex") val uv: Int
)

/**
 * @see [CurrentWeatherResponse] for field meanings
 */
internal class HourlyForecastResponse(
	@SerializedName("EpochDateTime") val dt: Int,
	@Flatten("Temperature.Value") val temp_c: Float,
	@Flatten("RealFeelTemperature.Value") val feelsLike_c: Float,
	@SerializedName("WeatherIcon") val condition: Int,
	@SerializedName("IsDaylight") val isDay: Boolean,
	@Flatten("Wind.Speed.Value") val wind_kph: Float,
	@Flatten("Wind.Direction.Degrees") val wind_degrees: Int,
	@Flatten("TotalLiquid.Value") val precip_mm: Int,
	@SerializedName("RelativeHumidity") val humidity: Int,
	@Flatten("DewPoint.Value") val dewPoint_c: Float,
	@SerializedName("CloudCover") val clouds: Int,
	@Flatten("Visibility.Value") val vis_km: Float,
	@SerializedName("PrecipitationProbability") val pop: Int,
	@SerializedName("UVIndex") val uv: Int
)

/**
 * @see CurrentWeatherResponse for fields meanings
 */
internal class DailyForecastResponse(
	@SerializedName("DailyForecasts") val forecasts: List<DailyForecast>
) {
	/**
	 * @param dt does not start at 00:00, but at a random? previous time
	 */
	class DailyForecast(
		@SerializedName("EpochDate") val dt: Int,
		@Flatten("Temperature.Minimum.Value") val minTemp_c: Float,
		@Flatten("Temperature.Maximum.Value") val maxTemp_c: Float,
		@Flatten("Day.Icon") val dayCondition: Int,
		@Flatten("Day.Wind.Speed.Value") val dayWind_kph: Float,
		@Flatten("Day.Wind.Direction.Degrees") val dayWind_degrees: Int,
		@Flatten("Day.TotalLiquid.Value") val dayPrecip_mm: Int,
		@Flatten("Day.PrecipitationProbability") val dayPop: Int,
		@Flatten("Day.CloudCover") val dayClouds: Int,
		@Flatten("Night.Icon") val nightCondition: Int,
		@Flatten("Night.Wind.Speed.Value") val nightWind_kph: Float,
		@Flatten("Night.Wind.Direction.Degrees") val nightWind_degrees: Int,
		@Flatten("Night.TotalLiquid.Value") val nightPrecip_mm: Int,
		@Flatten("Night.PrecipitationProbability") val nightPop: Int,
		@Flatten("Night.CloudCover") val nightClouds: Int,
		@Flatten("AirAndPollen.5.Value") val uv: Int,
		@Flatten("Sun.EpochRise") val sunrise: Int,
		@Flatten("Sun.EpochSet") val sunset: Int,
		@Flatten("Moon.EpochRise") val moonrise: Int,
		@Flatten("Moon.EpochSet") val moonset: Int,
		@Flatten("Moon.Age") val moonAge: Int,
	)
}