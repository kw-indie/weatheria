package asceapps.weatheria.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
class LocationEntity(
	@PrimaryKey val id: Int,
	val lat: Float,
	val lng: Float,
	@ColumnInfo(collate = ColumnInfo.UNICODE) val name: String,
	val country: String,
	val zoneOffset: Int,
	var pos: Int = 0
)