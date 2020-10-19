package asceapps.weatheria.data

import android.content.Context
import asceapps.weatheria.api.WeatherService

class WeatherInfoRepo private constructor(
	private val dao: WeatherInfoDao,
	private val service: WeatherService
) {

	fun loadAllInfo() = dao.loadAllInfo()

	suspend fun fetch(lat: String, lng: String) = WeatherInfo(
		service.current(lat, lng)
	)

	suspend fun fetch(query: String) = WeatherInfo(
		service.current(query)
	)

	suspend fun fetchUpdate(locationId: Int) = WeatherInfo.Update(
		service.current(locationId)
	)

	suspend fun save(info: WeatherInfo) = dao.insert(info.location, info.current)

	suspend fun update(info: WeatherInfo.Update) = dao.update(info)

	suspend fun delete(locationId: Int) = dao.delete(locationId)

	suspend fun retain(locationId: Int) = dao.retain(locationId)

	suspend fun deleteAll() = dao.deleteAll()

	suspend fun find(lat: String, lng: String) = service.find(lat, lng)

	suspend fun find(query: String) = service.find(query)

	suspend fun current(lat: String, lng: String) = service.current(lat, lng)

	suspend fun current(query: String) = service.current(query)

	suspend fun current(locationId: Int) = service.current(locationId)

	companion object {

		@Volatile
		private var instance: WeatherInfoRepo? = null

		fun getInstance(context: Context) =
			instance ?: synchronized(this) {
				instance ?: WeatherInfoRepo(
					AppDB.build(context).weatherInfoDao(),
					WeatherService.create(context)
				).also {instance = it}
			}
	}
}