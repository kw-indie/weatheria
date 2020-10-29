package asceapps.weatheria.api

class FindResponse(val list: List<Location>) {

	class Location(
		val id: Int,
		val coord: Coord,
		val name: String,
		val sys: Sys,
		val main: Main,
		val weather: List<WeatherCondition>
	) {

		class Coord(val lat: Float, val lon: Float)

		class Sys(val country: String)

		class Main(val temp: Float)
	}
}