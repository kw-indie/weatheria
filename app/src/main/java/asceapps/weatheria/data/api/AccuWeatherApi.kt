package asceapps.weatheria.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AccuWeatherApi {

	companion object {
		const val BASE_URL = "https://dataservice.accuweather.com"
		const val KEY_PARAM = "apikey"

		// todo conditions and icons
		val CONDITIONS = intArrayOf()
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