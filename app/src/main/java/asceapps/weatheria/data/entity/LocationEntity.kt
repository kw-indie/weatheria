package asceapps.weatheria.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import asceapps.weatheria.data.base.BaseLocation

@Entity(tableName = "locations")
class LocationEntity(
	@PrimaryKey override val id: Int,
	override val lat: Float,
	override val lng: Float,
	override val name: String,
	override val country: String,
	val zoneOffset: Int,
	var pos: Int = 0
): BaseLocation