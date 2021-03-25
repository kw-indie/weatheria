package asceapps.weatheria.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

	@GET("onecall?exclude=minutely")
	suspend fun oneCall(@Query("lat") lat: String, @Query("lon") lng: String): OneCallResponse

	@GET("find")
	suspend fun find(@Query("q") name: String): FindResponse

	@GET("find")
	suspend fun find(@Query("lat") lat: String, @Query("lon") lng: String): FindResponse

	companion object {

		const val ICON_URL_FORMAT = "https://openweathermap.org/img/wn/%s@2x.png"
	}
}