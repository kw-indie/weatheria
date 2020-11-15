package asceapps.weatheria.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = Table.LOCATIONS)
class LocationEntity(
	@PrimaryKey @ColumnInfo(name = Column.ID) val id: Int,
	val lat: Float,
	val lng: Float,
	@ColumnInfo(name = Column.NAME, collate = ColumnInfo.UNICODE) val name: String,
	val country: String
)