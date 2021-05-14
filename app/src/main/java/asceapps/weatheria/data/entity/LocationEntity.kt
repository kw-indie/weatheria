package asceapps.weatheria.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import asceapps.weatheria.data.base.BaseLocation

@Entity(tableName = "locations")
class LocationEntity(
	@PrimaryKey override val id: Int,
	override val lat: Float,
	override val lng: Float,
	val name: String,
	val country: String,
	val zoneOffset: Int,
	val pos: Int
): BaseLocation