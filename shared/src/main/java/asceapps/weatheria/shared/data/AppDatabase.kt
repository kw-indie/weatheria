package asceapps.weatheria.shared.data

import androidx.room.Database
import androidx.room.RoomDatabase
import asceapps.weatheria.shared.BuildConfig
import asceapps.weatheria.shared.data.dao.WeatherInfoDao
import asceapps.weatheria.shared.data.entity.CurrentEntity
import asceapps.weatheria.shared.data.entity.DailyEntity
import asceapps.weatheria.shared.data.entity.HourlyEntity
import asceapps.weatheria.shared.data.entity.LocationEntity

@Database(
	entities = [
		LocationEntity::class,
		CurrentEntity::class,
		HourlyEntity::class,
		DailyEntity::class
	],
	version = 1,
	exportSchema = BuildConfig.BUILD_TYPE == "debug"
)
abstract class AppDatabase: RoomDatabase() {

	companion object {
		const val DB_NAME = "weatheria"
	}

	abstract fun weatherInfoDao(): WeatherInfoDao
}