package asceapps.weatheria.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import asceapps.weatheria.shared.data.repo.Error
import asceapps.weatheria.shared.data.repo.Loading
import asceapps.weatheria.shared.data.repo.Success
import asceapps.weatheria.shared.data.repo.WeatherInfoRepo
import asceapps.weatheria.ui.widget.AppWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect

@HiltWorker
class RefreshWorker @AssistedInject constructor(
	@Assisted context: Context,
	@Assisted params: WorkerParameters,
	private val repo: WeatherInfoRepo
): CoroutineWorker(context, params) {

	override suspend fun doWork(): Result = coroutineScope {
		try {
			repo.refreshAll().collect {
				when(it) {
					// todo when kotlin formatting keeps empty blocks in one line, remove `Unit`
					is Loading -> Unit /* todo show progress notification */
					is Success -> AppWidget.sendUpdateBroadcast(applicationContext)
					is Error -> throw it.t
				}
			}
			Result.success()
		} catch(e: Exception) {
			e.printStackTrace()
			Result.retry()
		}
	}
}