package asceapps.weatheria.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
	tableName = "daily",
	primaryKeys = ["locationId", "dt"],
	foreignKeys = [
		ForeignKey(
			entity = SavedLocationEntity::class,
			parentColumns = ["id"],
			childColumns = ["locationId"],
			onDelete = ForeignKey.CASCADE
		)
	]
)
class DailyEntity(
	val locationId: Int,
	val dt: Int,
	val conditionId: Int,
	val windSpeed: Float,
	val windDir: Int,
	val pressure: Int,
	val humidity: Int,
	val dewPoint: Int,
	val clouds: Int,
	val minTemp: Int,
	val maxTemp: Int,
	val mornTemp: Int,
	val dayTemp: Int,
	val eveTemp: Int,
	val nightTemp: Int,
	val mornFeel: Int,
	val dayFeel: Int,
	val eveFeel: Int,
	val nightFeel: Int,
	val sunrise: Int,
	val sunset: Int,
	val pop: Int,
	val uvi: Float,
	val rain: Float? = null,
	val snow: Float? = null
)