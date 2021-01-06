package asceapps.weatheria.ui.settings

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import asceapps.weatheria.data.WeatherInfoRepo
import kotlinx.coroutines.coroutineScope

class AutoRefreshWorker @WorkerInject constructor(
	@Assisted context: Context,
	@Assisted params: WorkerParameters,
	private val repo: WeatherInfoRepo
): CoroutineWorker(context, params) {

	override suspend fun doWork(): Result = coroutineScope {
		try {
			repo.getSavedLocations()
				.value
				?.forEach {
					with(it) {
						repo.refresh(id, lat, lng)
					}
				}
			Result.success()
		} catch(e: Exception) {
			Result.failure()
		}
	}
}