package asceapps.weatheria.data.api

import androidx.annotation.Keep
import asceapps.weatheria.data.IDed

@Keep
class FindResponse(
	val list: List<Location>
) {

	class Location(
		override val id: Int,
		val dt: Int,
		val name: String,
		val coord: Coord,
		val main: Main,
		val wind: Wind,
		val sys: Sys,
		val weather: List<OneCallResponse.Weather>
	) : IDed

	class Coord(
		val lat: Float,
		val lon: Float
	)

	class Main(
		val temp: Float,
		val feels_like: Float,
		val pressure: Int,
		val humidity: Int
	)

	class Wind(
		val speed: Float,
		val deg: Int
	)

	class Sys(
		val country: String
	)
}