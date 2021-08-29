package asceapps.weatheria.shared.data.model

import asceapps.weatheria.shared.data.base.Listable
import java.time.ZoneId

class Location internal constructor(
	override val id: Int,
	val lat: Float,
	val lng: Float,
	val name: String,
	val country: String,
	val zoneId: ZoneId
): Listable {
	override val hash get() = id
}