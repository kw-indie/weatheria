package asceapps.weatheria.data

import androidx.lifecycle.liveData
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Singleton
class LocationRepo @Inject constructor(private val dao: LocationDao) {

	private val coordinateRegex = "^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?)\\s*,\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)\$"

	fun search(query: String, limit: Int = 30) = when {
		query.isEmpty() -> liveData {emit(emptyList<LocationEntity>())}
		query.matches(Regex(coordinateRegex)) -> {
			val (lat, lng) = query.split(',')
				.map {it.toFloat()}
			// Radius calculation logic:
			// Since our radius is not in distance but in coordinate points,
			// points closer to the equator will cover a much wider area with a static radius,
			// that's why, we will take smaller values near the equator and bigger ones near the poles.
			// min value: 0.1 near the equator, max value: 4 at the poles
			val radiansLat = abs(lat) / 180 * PI.toFloat()
			val radius = (sin(radiansLat) * 4).coerceAtLeast(0.1f)
			dao.find(
				lat, lng,
				lat - radius,
				lat + radius,
				lng - radius,
				lng + radius,
				limit
			)
		}
		else -> dao.find(query, limit)
	}
}