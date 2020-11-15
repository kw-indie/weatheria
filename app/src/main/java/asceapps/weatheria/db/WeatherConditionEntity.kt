package asceapps.weatheria.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = Table.CONDITIONS)
class WeatherConditionEntity(
	@PrimaryKey @ColumnInfo(name = Column.ID) val id: Int,
	val descEn: String,
	val descAr: String,
	val icon: String
)