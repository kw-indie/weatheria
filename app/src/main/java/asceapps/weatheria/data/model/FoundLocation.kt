package asceapps.weatheria.data.model

import asceapps.weatheria.data.base.BaseLocation
import asceapps.weatheria.data.base.Listable

class FoundLocation(
	override val id: Int,
	override val lat: Float,
	override val lng: Float,
	val name: String,
	val country: String,
	val temp: Float,
	val feelsLike: Float,
	val pressure: Int,
	val humidity: Int,
	val windSpeed: Float,
	val windDir: Int
): BaseLocation, Listable {

	override fun hashCode() = id
}