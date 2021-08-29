package asceapps.weatheria.shared.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
	tableName = "hourly",
	primaryKeys = ["locationId", "dt"],
	foreignKeys = [
		ForeignKey(
			entity = LocationEntity::class,
			parentColumns = ["id"],
			childColumns = ["locationId"],
			onDelete = ForeignKey.CASCADE
		)
	]
)
internal class HourlyEntity(
	val locationId: Int,
	val dt: Int,
	val temp: Int,
	val feelsLike: Int,
	val condition: Int,
	val isDay: Boolean,
	val windSpeed: Float,
	val windDir: Int,
	val humidity: Int,
	val dewPoint: Int,
	val clouds: Int,
	val visibility: Float,
	val pop: Int,
	val uv: Int
)