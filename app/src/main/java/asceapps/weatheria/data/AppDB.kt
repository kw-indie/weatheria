package asceapps.weatheria.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Location::class, CurrentWeather::class], version = 1)
abstract class AppDB: RoomDatabase() {

	abstract fun weatherInfoDao(): WeatherInfoDao

	companion object {

		private const val DB_NAME: String = "weatheria-db"

		fun build(context: Context) =
			Room.databaseBuilder(context.applicationContext, AppDB::class.java, DB_NAME).build()
	}
}