package asceapps.weatheria.model

import android.content.Context
import asceapps.weatheria.api.WeatherService
import asceapps.weatheria.db.AppDB
import asceapps.weatheria.db.WeatherConditionEntity
import asceapps.weatheria.db.WeatherInfoDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class WeatherInfoRepo private constructor(
	private val dao: WeatherInfoDao,
	private val service: WeatherService
) {

	fun getAll() = dao.getAllInfo()
		.distinctUntilChanged()
		.flowOn(Dispatchers.IO)
		.map {infoList ->
			val conditionIds = mutableListOf<Int>()
			infoList.forEach {info -> conditionIds += Mapper.extractConditionIds(info)}
			val conditions = dao.getConditions(conditionIds)
				.associateBy {it.id}
			infoList.map {info -> Mapper.entityToModel(conditions, info)}
		}

	suspend fun find(lat: String, lng: String) = Mapper.dtoToModel(service.find(lat, lng))

	suspend fun find(query: String) = Mapper.dtoToModel(service.find(query))

	suspend fun get(lat: String, lng: String): WeatherInfo {
		// todo search locally first
		val findResp = service.find(lat, lng)
		val foundLocations = Mapper.dtoToModel(findResp)
		// todo let user choose a location
		val selectedLocation = foundLocations[0]
		val oneCallResp = service.oneCall(
			selectedLocation.lat.toString(),
			selectedLocation.lng.toString())
		val conditionsMap = HashMap<Int, WeatherConditionEntity>()
		val weatherInfoEntity = Mapper.dtoToEntity(selectedLocation, oneCallResp, conditionsMap)
		with(weatherInfoEntity) {
			dao.insert(conditionsMap.values.toList(), location, current, hourly, daily)
		}
		return Mapper.entityToModel(conditionsMap, weatherInfoEntity)
	}

	suspend fun get(query: String): WeatherInfo {
		// todo search locally first
		// todo reuse code from above
		val findResp = service.find(query)
		val foundLocations = Mapper.dtoToModel(findResp)
		// todo let user choose a location
		val selectedLocation = foundLocations[0]
		val oneCallResp = service.oneCall(
			selectedLocation.lat.toString(),
			selectedLocation.lng.toString())
		val conditionsMap = HashMap<Int, WeatherConditionEntity>()
		val weatherInfoEntity = Mapper.dtoToEntity(selectedLocation, oneCallResp, conditionsMap)
		with(weatherInfoEntity) {
			dao.insert(conditionsMap.values.toList(), location, current, hourly, daily)
		}
		return Mapper.entityToModel(conditionsMap, weatherInfoEntity)
	}

	suspend fun getUpdate(l: Location) {
		val oneCallResp = service.oneCall(l.lat.toString(), l.lng.toString())
		val conditionsMap = HashMap<Int, WeatherConditionEntity>()
		val current = Mapper.extractCurrentEntity(l.id, oneCallResp.current!!, conditionsMap)
		val hourly = Mapper.extractHourlyEntity(l.id, oneCallResp.hourly!!, conditionsMap)
		val daily = Mapper.extractDailyEntity(l.id, oneCallResp.daily!!, conditionsMap)
		dao.update(conditionsMap.values.toList(), current, hourly, daily)
	}

	suspend fun delete(l: Location) = dao.delete(l.id)

	suspend fun deleteAll() = dao.deleteAll()

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