package asceapps.weatheria.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
abstract class WeatherInfoDao {

	// our use case forgives us for:
	// 1- no pagination, since query result is expected to be small
	// 2- no separate calls for each table since all available info is almost always required
	// also, live data is retrieved async, no need for suspend
	@Query("SELECT * FROM " + Location.TABLE_NAME + " AS L " +
		"INNER JOIN " + CurrentWeather.TABLE_NAME + " AS CW " +
		"ON CW." + CurrentWeather.COL_ID + " == L." + Location.COL_ID + " " +
		"ORDER BY L." + Location.COL_CREATED_AT + " DESC")
	abstract fun getAllInfo(): LiveData<List<WeatherInfo>>

	// can make return type int[] to return ids of inserted rows, but  our api provides the ids as well.
	// all these additions are made in a single transaction.
	// we made all inserts in a single transaction because usually we never insert any of this info alone.
	// future info should be added to this methods parameters.
	// should probably remove the conflict strategy, since we don't want you to use insert for updates.
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract suspend fun insert(l: Location, cw: CurrentWeather)

	// no update for location since it's static once retrieved from api and inserted into db.
	// overload other update methods.
	@Transaction
	open suspend fun update(newInfo: WeatherInfo.Update) {
		update(newInfo.location)
		update(newInfo.current)
	}

	@Update(entity = Location::class)
	abstract suspend fun update(l: Location.Update)

	@Update
	abstract suspend fun update(cw: CurrentWeather)

	// rely on FK cascade to delete related info
	@Query("DELETE FROM " + Location.TABLE_NAME + " WHERE " + Location.COL_ID + " = :locationId")
	abstract suspend fun delete(locationId: Int)

	@Query("DELETE FROM " + Location.TABLE_NAME + " WHERE " + Location.COL_ID + " != :locationId")
	abstract suspend fun retain(locationId: Int)

	@Query("DELETE FROM " + Location.TABLE_NAME)
	abstract suspend fun deleteAll()
}