package asceapps.weatheria.api

/**
 * @param dt current time
 * @param id location id in web service
 * @param timezone timezone offset
 */
class CurrentResponse(
	val dt: Int,
	val coord: Coord,
	val weather: List<WeatherCondition>,
	val main: Main,
	val visibility: Int,
	val wind: Wind,
	val sys: Sys,
	val timezone: Int,
	val id: Int,
	val name: String
) {

	class Coord(val lat: Float, val lon: Float)

	class Main(val temp: Float, val feel_like: Float, val pressure: Int, val humidity: Int)

	class Wind(val speed: Float, val deg: Int)

	class Sys(val country: String, val sunrise: Int, val sunset: Int)
}