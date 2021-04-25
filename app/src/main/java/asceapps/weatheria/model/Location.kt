package asceapps.weatheria.model

import java.time.ZoneOffset

class Location(
	val id: Int,
	val lat: Float,
	val lng: Float,
	val name: String,
	val country: String,
	val zoneOffset: ZoneOffset,
	val pos: Int
)