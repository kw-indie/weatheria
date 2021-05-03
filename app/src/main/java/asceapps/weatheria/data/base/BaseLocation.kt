package asceapps.weatheria.data.base

abstract class BaseLocation(
	open val id: Int,
	val lat: Float,
	val lng: Float,
	val name: String,
	val country: String
)