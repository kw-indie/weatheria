package asceapps.weatheria.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
	tableName = Table.LOCATIONS,
	indices = [Index(Column.POS, unique = true)]
)
class LocationEntity(
	@PrimaryKey @ColumnInfo(name = Column.LOC_ID) val id: Int,
	val lat: Float,
	val lng: Float,
	val name: String,
	val country: String,
	val zoneOffset: Int,
	@ColumnInfo(name = Column.POS) var pos: Int = 0
)