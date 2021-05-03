package asceapps.weatheria.model

import asceapps.weatheria.data.base.BaseLocation
import java.time.ZoneOffset

class Location(
	id: Int,
	lat: Float,
	lng: Float,
	name: String,
	country: String,
	val zoneOffset: ZoneOffset,
	val pos: Int
): BaseLocation(id, lat, lng, name, country)