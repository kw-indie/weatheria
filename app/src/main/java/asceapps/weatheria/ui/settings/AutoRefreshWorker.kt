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
			repo.refreshAll()
			Result.success()
		} catch(e: Exception) {
			Result.failure()
		}
	}
}