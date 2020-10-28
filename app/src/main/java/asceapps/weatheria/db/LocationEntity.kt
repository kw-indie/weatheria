package asceapps.weatheria.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = Table.LOCATIONS)
class LocationEntity(
	@PrimaryKey @ColumnInfo(name = Column.LOC_ID) val id: Int,
	val lat: Float,
	val lng: Float,
	val name: String,
	val country: String,
	val zoneOffset: Int,
	@ColumnInfo(name = Column.POS, index = true) val pos: Int = 0
)