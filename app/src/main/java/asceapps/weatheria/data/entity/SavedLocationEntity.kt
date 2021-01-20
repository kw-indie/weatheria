package asceapps.weatheria.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_locations")
class SavedLocationEntity(
	@PrimaryKey val id: Int,
	val lat: Float,
	val lng: Float,
	val name: String,
	val country: String,
	val zoneOffset: Int,
	var pos: Int = 0
)