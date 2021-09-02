package asceapps.weatheria.shared.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
	tableName = "daily",
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
internal class DailyEntity(
	val locationId: Int,
	val dt: Int,
	val minTemp: Int,
	val maxTemp: Int,
	val dayCondition: Int,
	val nightCondition: Int,
	val dayWindSpeed: Float,
	val nightWindSpeed: Float,
	val dayPop: Int,
	val nightPop: Int,
	val dayClouds: Int,
	val nightClouds: Int,
	val uv: Int,
	val sunrise: Int,
	val sunset: Int,
	val moonrise: Int,
	val moonset: Int,
	val moonPhaseIndex: Int
)