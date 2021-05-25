package asceapps.weatheria.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

	/**
	 * @param q latLng, name, US zip, UK postcode, [AUTO_IP], ip address, or others
	 */
	@GET("search.json")
	suspend fun search(@Query("q") q: String): List<SearchResponse>

	/**
	 * @param q see [search] for possible values
	 * @param days 1-10. free account gets only 3 max
	 * @param aqi include air quality report. [YES] or [NO]
	 * @param alerts include alerts. values similar to `aqi`
	 * We can specify unixdt unix seconds at which the forecast data starts (within next 10 days only).
	 * but it wil return only 1 forecast day.
	 */
	@GET("forecast.json")
	suspend fun forecast(
		@Query("q") q: String,
		@Query("days") days: Int,
		@Query("aqi") aqi: String,
		@Query("alerts") alerts: String
	): ForecastResponse
}

const val AUTO_IP = "auto:ip"
const val YES = "yes"
const val NO = "no"
const val EN = "en"
const val AR = "ar"
const val TR = "tr"

private val CONDITIONS = intArrayOf(
	1000, 1003, 1006, 1009, 1030, 1063, 1066, 1069, 1072, 1087, 1114, 1117, 1135, 1147, 1150, 1153,
	1168, 1171, 1180, 1183, 1186, 1189, 1192, 1195, 1198, 1201, 1204, 1207, 1210, 1213, 1216, 1219,
	1222, 1225, 1237, 1240, 1243, 1246, 1249, 1252, 1255, 1258, 1261, 1264, 1273, 1276, 1279, 1282
)
private val CONDITION_ICONS = intArrayOf(
	113, 116, 119, 122, 143, 176, 179, 182, 185, 200, 227, 230, 248, 260, 263, 266, 281, 284, 293,
	296, 299, 302, 305, 308, 311, 314, 317, 320, 323, 326, 329, 332, 335, 338, 350, 353, 356, 359,
	362, 365, 368, 371, 374, 377, 386, 389, 392, 395
)

fun conditionIndex(condition: Int) = CONDITIONS.binarySearch(condition)

fun conditionIcon(conditionIndex: Int, isDay: Boolean? = null) =
	"w${CONDITION_ICONS[conditionIndex]}" + when(isDay) {
		false -> "n"
		else -> "d"
	}