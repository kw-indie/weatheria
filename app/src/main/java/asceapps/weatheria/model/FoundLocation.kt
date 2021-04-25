package asceapps.weatheria.model

class FoundLocation(
	override val id: Int,
	val lat: Float,
	val lng: Float,
	val name: String,
	val country: String,
	val temp: Float,
	val feelsLike: Float,
	val pressure: Int,
	val humidity: Int,
	val windSpeed: Float,
	val windDir: Int
): IDed {

	override fun hashCode() = id
}