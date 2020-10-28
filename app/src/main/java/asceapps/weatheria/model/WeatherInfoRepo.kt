package asceapps.weatheria.model

import android.content.Context
import asceapps.weatheria.api.WeatherService
import asceapps.weatheria.data.WeatherInfo
import asceapps.weatheria.db.AppDB
import asceapps.weatheria.db.WeatherConditionEntity
import asceapps.weatheria.db.WeatherInfoDao

class WeatherInfoRepo private constructor(
	private val dao: WeatherInfoDao,
	private val service: WeatherService
) {

	fun loadAllInfo() = dao.loadAllInfo()

	fun getAll() = dao.getAll()

	suspend fun fetch(lat: String, lng: String) = WeatherInfo(service.current(lat, lng))

	suspend fun get(lat: String, lng: String): asceapps.weatheria.model.WeatherInfo {
		// todo search locally first
		val findResp = service.find(lat, lng)
		val foundLocations = Mapper.dtoToModel(findResp)
		// todo let user choose a location
		val selectedLocation = foundLocations[0]
		val oneCallResp = service.currentNew(
			selectedLocation.lat.toString(),
			selectedLocation.lng.toString())
		val conditionsMap = HashMap<Int, WeatherConditionEntity>()
		val weatherInfoEntity = Mapper.dtoToEntity(selectedLocation, oneCallResp, conditionsMap)
		dao.insertLocation(weatherInfoEntity.location)
		dao.insertCurrent(weatherInfoEntity.current)
		dao.insertHourly(weatherInfoEntity.hourly)
		dao.insertDaily(weatherInfoEntity.daily)
		dao.insertWeatherCondition(conditionsMap.values.toList())
		return Mapper.entityToModel(conditionsMap, weatherInfoEntity)
	}

	suspend fun fetch(query: String) = WeatherInfo(
		service.current(query)
	)

	suspend fun get(query: String): asceapps.weatheria.model.WeatherInfo {
		// todo search locally first
		// todo reuse code from above
		val findResp = service.find(query)
		val foundLocations = Mapper.dtoToModel(findResp)
		// todo let user choose a location
		val selectedLocation = foundLocations[0]
		val oneCallResp = service.currentNew(
			selectedLocation.lat.toString(),
			selectedLocation.lng.toString())
		val conditionsMap = HashMap<Int, WeatherConditionEntity>()
		val weatherInfoEntity = Mapper.dtoToEntity(selectedLocation, oneCallResp, conditionsMap)
		dao.insertLocation(weatherInfoEntity.location)
		dao.insertCurrent(weatherInfoEntity.current)
		dao.insertHourly(weatherInfoEntity.hourly)
		dao.insertDaily(weatherInfoEntity.daily)
		dao.insertWeatherCondition(conditionsMap.values.toList())
		return Mapper.entityToModel(conditionsMap, weatherInfoEntity)
	}

	suspend fun fetchUpdate(locationId: Int) = WeatherInfo.Update(
		service.current(locationId)
	)

	suspend fun getUpdate(l: Location) {
		val oneCallResp = service.currentNew(l.lat.toString(), l.lng.toString())
		val conditionsMap = HashMap<Int, WeatherConditionEntity>()
		val current = Mapper.extractCurrentEntity(l.id, oneCallResp.current!!, conditionsMap)
		val hourly = Mapper.extractHourlyEntity(l.id, oneCallResp.hourly!!, conditionsMap)
		val daily = Mapper.extractDailyEntity(l.id, oneCallResp.daily!!, conditionsMap)
		dao.insertCurrent(current)
		dao.insertHourly(hourly)
		dao.insertDaily(daily)
		dao.insertWeatherCondition(conditionsMap.values.toList())
	}

	suspend fun save(info: WeatherInfo) = dao.insert(info.location, info.current)

	suspend fun update(info: WeatherInfo.Update) = dao.update(info)

	suspend fun delete(locationId: Int) = dao.delete(locationId)

	suspend fun retain(locationId: Int) = dao.retain(locationId)

	suspend fun deleteAll() = dao.deleteAll()

	suspend fun find(lat: String, lng: String) = Mapper.dtoToModel(service.find(lat, lng))

	suspend fun find(query: String) = Mapper.dtoToModel(service.find(query))

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