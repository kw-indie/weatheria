package asceapps.weatheria.data.repo

import androidx.lifecycle.liveData
import asceapps.weatheria.data.dao.LocationDao
import asceapps.weatheria.data.entity.LocationEntity
import asceapps.weatheria.di.IoDispatcher
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

@ActivityRetainedScoped
class LocationRepo @Inject constructor(
	private val dao: LocationDao,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

	private val coordinateRegex =
		Regex("^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?)\\s*,\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)\$")

	fun search(query: String, limit: Int = 30) = liveData(ioDispatcher) {
		emit(Result.Loading)
		when {
			query.isEmpty() -> emit(Result.Success(emptyList<LocationEntity>()))
			query.matches(coordinateRegex) -> {
				val (lat, lng) = query.split(',')
					.map { it.toFloat() }
				// Radius calculation logic:
				// Since our radius is not in distance but in coordinate points,
				// points closer to the equator will cover a much wider area with a static radius,
				// that's why, we will take smaller values near the equator and bigger ones near the poles.
				// min value: 0.15 (~16km) at the equator, max value: 1 at the poles
				val radiansLat = abs(lat) / 180 * PI.toFloat()
				val radius = sin(radiansLat).pow(4).coerceAtLeast(0.15f)
				val list = dao.find(
					lat, lng,
					lat - radius,
					lat + radius,
					lng - radius,
					lng + radius,
					limit
				)
				emit(Result.Success(list))
			}
			else -> emit(Result.Success(dao.find(query, limit)))
		}
	}
}