package asceapps.weatheria.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import asceapps.weatheria.data.entity.LocationEntity
// todo change return type when downstream converts to flow
@Dao
interface LocationDao {

	@Query("SELECT * FROM locations WHERE name LIKE :locationName || '%' ORDER BY name LIMIT :limit")
	fun find(locationName: String, limit: Int): LiveData<List<LocationEntity>>

	@Query(
		"SELECT * FROM locations WHERE lat BETWEEN :bot AND :top AND lng BETWEEN :left AND :right ORDER BY (lat-:lat)*(lat-:lat)+(lng-:lng)*(lng-:lng) LIMIT :limit")
	fun find(
		lat: Float, lng: Float, bot: Float, top: Float, left: Float, right: Float, limit: Int
	): LiveData<List<LocationEntity>>
}