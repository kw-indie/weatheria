package asceapps.weatheria.data.api

import androidx.annotation.Keep

@Keep
class FindResponse(
	val list: List<Location>
) {

	/**
	 * @param id location id
	 * @param dt time and date of weather info, UTC, unix seconds
	 */
	@Keep
	class Location(
		val id: Int,
		val dt: Int,
		val name: String,
		val coord: Coord,
		val main: Main,
		val wind: Wind,
		val sys: Sys,
		val weather: List<OneCallResponse.Weather>
	)

	@Keep
	class Coord(
		val lat: Float,
		val lon: Float
	)

	@Keep
	class Main(
		val temp: Float,
		val feels_like: Float,
		val pressure: Int,
		val humidity: Int
	)

	@Keep
	class Wind(
		val speed: Float,
		val deg: Int
	)

	@Keep
	class Sys(
		val country: String
	)
}