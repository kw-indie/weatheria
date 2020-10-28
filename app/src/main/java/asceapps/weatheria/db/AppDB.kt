package asceapps.weatheria.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import asceapps.weatheria.BuildConfig
import asceapps.weatheria.data.CurrentWeather
import asceapps.weatheria.data.Location

@Database(
	entities = [
		Location::class, CurrentWeather::class,
		LocationEntity::class,
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

		fun build(context: Context) =
			Room.databaseBuilder(context.applicationContext, AppDB::class.java, "weatheria-db")
				.fallbackToDestructiveMigration()
				.build()
	}
}