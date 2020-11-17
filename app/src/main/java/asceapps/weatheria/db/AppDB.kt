package asceapps.weatheria.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import asceapps.weatheria.BuildConfig

@Database(
	entities = [
		LocationEntity::class,
		SavedLocationEntity::class,
		CurrentEntity::class,
		HourlyEntity::class,
		DailyEntity::class,
		WeatherConditionEntity::class
	],
	version = 1,
	exportSchema = BuildConfig.BUILD_TYPE == "debug"
)
abstract class AppDB: RoomDatabase() {

	abstract fun weatherInfoDao(): WeatherInfoDao

	companion object {

		fun build(appContext: Context) =
			Room.databaseBuilder(appContext, AppDB::class.java, "weatheria")
				.createFromAsset("database/weatheria.db")
				.build()
	}
}