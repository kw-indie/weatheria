package asceapps.weatheria.model

import asceapps.weatheria.data.IDed
import java.time.ZoneOffset

class Location(
	override val id: Int,
	val lat: Float,
	val lng: Float,
	val name: String,
	val country: String,
	val zoneOffset: ZoneOffset,
	val pos: Int
) : IDed {

	// for HashItemCallback
	override fun hashCode() = id
}