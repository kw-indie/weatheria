package asceapps.weatheria.db

import androidx.room.ColumnInfo

abstract class BaseData(
	@ColumnInfo(name = Column.LOC_ID) val locationId: Int,
	@ColumnInfo(name = Column.DT) val dt: Int,
	val conditionId: Int,
	val windSpeed: Float,
	val windDir: Int,
	val pressure: Int,
	val humidity: Int,
	val dewPoint: Int,
	val clouds: Int
)