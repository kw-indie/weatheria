package asceapps.weatheria.shared.data.model

import asceapps.weatheria.shared.data.base.Listable
import asceapps.weatheria.shared.data.util.countryFlag
import java.time.ZoneId

class Location internal constructor(
	override val id: Int,
	val lat: Float,
	val lng: Float,
	val name: String,
	val country: String,
	val cc: String,
	val zoneId: ZoneId
): Listable {
	override val hash get() = id

	/**
	 * country unicode flag or subscript uppercase country code
	 * eg. `🇹🇷` for Turkey
	 */
	val countryFlag get() = countryFlag(cc)
}