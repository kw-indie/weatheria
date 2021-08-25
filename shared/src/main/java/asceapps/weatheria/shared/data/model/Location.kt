package asceapps.weatheria.shared.data.model

import asceapps.weatheria.shared.data.base.BaseLocation
import java.time.Instant
import java.time.ZoneId

class Location(
	override val id: Int,
	override val lat: Float,
	override val lng: Float,
	override val name: String,
	override val country: String,
	val zoneId: ZoneId,
	val lastUpdate: Instant,
	val pos: Int
): BaseLocation