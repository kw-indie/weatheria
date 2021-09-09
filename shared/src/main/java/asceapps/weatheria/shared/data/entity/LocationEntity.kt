package asceapps.weatheria.shared.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
internal class LocationEntity(
	@PrimaryKey val id: Int,
	val lat: Float,
	val lng: Float,
	val name: String,
	val country: String,
	val cc: String,
	val zoneId: String,
	val lastUpdate: Int,
	val pos: Int
)