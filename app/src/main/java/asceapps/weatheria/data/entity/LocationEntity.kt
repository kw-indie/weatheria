package asceapps.weatheria.data.entity

import androidx.room.Entity
import asceapps.weatheria.data.base.BaseLocation

@Entity(
	tableName = "locations",
	primaryKeys = ["id"]
)
class LocationEntity(
	id: Int,
	lat: Float,
	lng: Float,
	name: String,
	country: String,
	val zoneOffset: Int,
	var pos: Int = 0
): BaseLocation(id, lat, lng, name, country)