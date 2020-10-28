package asceapps.weatheria.model

class FoundLocation(
	val id: Int,
	val lat: Float,
	val lng: Float,
	val name: String,
	val country: String,
	val temp: Int,
	val weather: String,
	val weatherIcon: String
)