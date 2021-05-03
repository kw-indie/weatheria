package asceapps.weatheria.model

import asceapps.weatheria.data.base.BaseLocation

class FoundLocation(
	override val id: Int,
	lat: Float,
	lng: Float,
	name: String,
	country: String,
	val temp: Float,
	val feelsLike: Float,
	val pressure: Int,
	val humidity: Int,
	val windSpeed: Float,
	val windDir: Int
): BaseLocation(id, lat, lng, name, country), IDed {

	override fun hashCode() = id
}