package asceapps.weatheria.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import asceapps.weatheria.BuildConfig
import asceapps.weatheria.data.dao.LocationDao
import asceapps.weatheria.data.dao.WeatherInfoDao
import asceapps.weatheria.data.entity.CurrentEntity
import asceapps.weatheria.data.entity.DailyEntity
import asceapps.weatheria.data.entity.HourlyEntity
import asceapps.weatheria.data.entity.LocationEntity
import asceapps.weatheria.data.entity.SavedLocationEntity

@Database(
	entities = [
		LocationEntity::class,
		SavedLocationEntity::class,
		CurrentEntity::class,
		HourlyEntity::class,
		DailyEntity::class
	],
	version = 1,
	exportSchema = BuildConfig.BUILD_TYPE == "debug"
)
abstract class AppDatabase: RoomDatabase() {

	abstract fun locationDao(): LocationDao
	abstract fun weatherInfoDao(): WeatherInfoDao

	companion object {

		fun build(appContext: Context) =
			Room.databaseBuilder(appContext, AppDatabase::class.java, "weatheria")
				.createFromAsset("database/weatheria.db")
				.build()
	}
}