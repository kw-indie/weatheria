package asceapps.weatheria.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import asceapps.weatheria.R
import asceapps.weatheria.data.model.WeatherInfo
import asceapps.weatheria.data.repo.Loading
import asceapps.weatheria.data.repo.SettingsRepo
import asceapps.weatheria.data.repo.Success
import asceapps.weatheria.data.repo.WeatherInfoRepo
import asceapps.weatheria.ui.MainActivity
import asceapps.weatheria.worker.RefreshWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class AppWidget: AppWidgetProvider() {

	companion object {
		// if i remove ACTION_APPWIDGET_UPDATE, the widget never gets seen by the system
		private const val ACTION_UPDATE_DATA = "action_update_widget_data"

		fun sendUpdateBroadcast(appContext: Context) {
			val ids = AppWidgetManager.getInstance(appContext)
				.getAppWidgetIds(ComponentName(appContext, AppWidget::class.java))
			val intent = Intent(appContext, AppWidget::class.java).apply {
				action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
				putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
			}
			appContext.sendBroadcast(intent)
		}
	}

	@Inject
	lateinit var infoRepo: WeatherInfoRepo

	@Inject
	lateinit var settingsRepo: SettingsRepo

	override fun onReceive(context: Context, intent: Intent) {
		if(intent.action == "${context.packageName}.$ACTION_UPDATE_DATA") {
			WorkManager.getInstance(context)
				.enqueue(OneTimeWorkRequestBuilder<RefreshWorker>().build())
			val awm = AppWidgetManager.getInstance(context)
			val ids = awm.getAppWidgetIds(ComponentName(context, AppWidget::class.java))
			loadingAnimation(context, awm, ids, true)
		} else super.onReceive(context, intent)
		// turns out if i don't call super, hilt won't inject
	}

	override fun onUpdate(context: Context, awm: AppWidgetManager, ids: IntArray) {
		GlobalScope.launch {
			infoRepo.getByPos(settingsRepo.selectedPos) // also react to unit sys change
				.take(2).collect { // take 'loading' and 'success/error' only
					loadingAnimation(context, awm, ids, it is Loading)
					if(it is Success) {
						updateWidgets(context, awm, ids, it.data)
					} /*else if (it is Error) {
						todo show error toast
					}*/
				}
		}
	}

	private fun loadingAnimation(context: Context, awm: AppWidgetManager, ids: IntArray, start: Boolean) {
		val views = RemoteViews(context.packageName, R.layout.app_widget).apply {
			setViewVisibility(R.id.iv_refresh, if(start) View.GONE else View.VISIBLE)
			setViewVisibility(R.id.progress, if(start) View.VISIBLE else View.GONE)
		}
		awm.partiallyUpdateAppWidget(ids, views)
	}

	private fun updateWidgets(context: Context, awm: AppWidgetManager, ids: IntArray, info: WeatherInfo) {
		val views = RemoteViews(context.packageName, R.layout.app_widget).apply {
			setTextViewText(R.id.tv_location, info.location.name)
			setTextViewText(R.id.tv_temp, info.currentTemp)
			setTextViewText(R.id.tv_humidity_value, info.currentHumidity)
			val uvString = context.getString(
				R.string.f_2s_p,
				info.currentUVIndex,
				context.resources.getStringArray(R.array.uv_levels)[info.currentUVLevelIndex]
			)
			setTextViewText(R.id.tv_uv_value, uvString)
			setImageViewResource(R.id.iv_icon, info.current.icon)
			setOnClickPendingIntent(R.id.iv_refresh, getUpdateDataBroadcastIntent(context))
			val lastUpdate = context.getString(R.string.f_last_update, info.lastUpdate)
			setTextViewText(R.id.tv_last_update, lastUpdate)
			removeAllViews(R.id.ll_forecasts)
			// todo move formatters to util
			val timeFormatter = DateTimeFormatter.ofPattern("h a")
			val dayFormatter = DateTimeFormatter.ofPattern("EEE")
			// add 3 views for hourly (each 6 hrs), and 2 for daily
			val items = info.hourly.take(24).filterIndexed { i, _ -> i % 6 == 0 }.drop(1).map {
				val text = LocalDateTime.ofInstant(it.hour, info.location.zoneId).format(timeFormatter)
				getItemRemoteView(context, it.icon, text)
			} + info.daily.drop(1).map {
				val text = LocalDateTime.ofInstant(it.date, info.location.zoneId).format(dayFormatter)
				getItemRemoteView(context, it.icon, text)
			}
			for(i in items) {
				addView(R.id.ll_forecasts, i)
			}
			setOnClickPendingIntent(R.id.root, getOpenAppIntent(context))
		}
		// Instruct the widget manager to update the widget
		awm.updateAppWidget(ids, views)
	}

	private fun getUpdateDataBroadcastIntent(context: Context) = PendingIntent.getBroadcast(
		context,
		0,
		Intent(context, AppWidget::class.java).apply {
			action = "${context.packageName}.$ACTION_UPDATE_DATA"
		},
		PendingIntent.FLAG_UPDATE_CURRENT
	)

	private fun getOpenAppIntent(context: Context) = PendingIntent.getActivity(
		context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT
	)

	private fun getItemRemoteView(context: Context, iconRes: Int, text: String) =
		RemoteViews(context.packageName, R.layout.item_widget_forecast).apply {
			setImageViewResource(R.id.iv_icon, iconRes)
			setTextViewText(R.id.tv_text, text)
		}
}
