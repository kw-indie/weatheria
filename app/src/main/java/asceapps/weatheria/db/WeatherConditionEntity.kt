package asceapps.weatheria.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = Table.CONDITIONS)
class WeatherConditionEntity(
	@PrimaryKey @ColumnInfo(name = Column.CONDITION_ID) val id: Int,
	val main: String,
	val desc: String,
	val icon: String
)