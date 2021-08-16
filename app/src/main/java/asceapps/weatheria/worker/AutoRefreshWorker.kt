package asceapps.weatheria.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import asceapps.weatheria.data.repo.Error
import asceapps.weatheria.data.repo.Loading
import asceapps.weatheria.data.repo.WeatherInfoRepo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect

@HiltWorker
class AutoRefreshWorker @AssistedInject constructor(
	@Assisted context: Context,
	@Assisted params: WorkerParameters,
	private val repo: WeatherInfoRepo
): CoroutineWorker(context, params) {

	override suspend fun doWork(): Result = coroutineScope {
		try {
			repo.refreshAll().collect {
				when(it) {
					is Loading -> { /* todo show progress notification */
					}
					is Error -> throw it.t
					else -> {
					}
				}
			}
			Result.success()
		} catch(e: Exception) {
			Result.retry()
		}
	}
}