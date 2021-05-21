package asceapps.weatheria.data.model

import asceapps.weatheria.data.base.BaseLocation
import java.time.Instant
import java.time.ZoneOffset

class Location(
	override val id: Int,
	override val lat: Float,
	override val lng: Float,
	val name: String,
	val country: String,
	val zoneOffset: ZoneOffset,
	val lastUpdate: Instant,
	val pos: Int
): BaseLocation