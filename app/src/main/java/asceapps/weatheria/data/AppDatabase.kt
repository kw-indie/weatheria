package asceapps.weatheria.data

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
		DailyEntity::class
	],
	version = 1,
	exportSchema = BuildConfig.BUILD_TYPE == "debug"
)
abstract class AppDatabase: RoomDatabase() {

	abstract fun weatherInfoDao(): WeatherInfoDao

	companion object {

		fun build(appContext: Context) =
			Room.databaseBuilder(appContext, AppDatabase::class.java, "weatheria")
				.createFromAsset("database/weatheria.db")
				.build()
	}
}