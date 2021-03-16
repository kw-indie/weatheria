package asceapps.weatheria.data.dao

import androidx.room.Dao
import androidx.room.Query
import asceapps.weatheria.data.entity.LocationEntity

@Dao
interface LocationDao {

	@Query(
		"SELECT * FROM locations WHERE name LIKE :locationName || '%' AND id NOT IN (SELECT id FROM saved_locations) ORDER BY name LIMIT :limit"
	)
	suspend fun find(locationName: String, limit: Int): List<LocationEntity>

	@Query(
		"SELECT * FROM locations WHERE lat BETWEEN :bot AND :top AND lng BETWEEN :left AND :right AND id NOT IN (SELECT id FROM saved_locations) ORDER BY (lat-:lat)*(lat-:lat)+(lng-:lng)*(lng-:lng) LIMIT :limit"
	)
	suspend fun find(
		lat: Float, lng: Float, bot: Float, top: Float, left: Float, right: Float, limit: Int
	): List<LocationEntity>
}