package asceapps.weatheria.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import asceapps.weatheria.R
import asceapps.weatheria.data.model.WeatherInfo
import asceapps.weatheria.data.repo.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class AppWidget: AppWidgetProvider() {

	@Inject
	lateinit var infoRepo: WeatherInfoRepo

	@Inject
	lateinit var settingsRepo: SettingsRepo
	private var job: Job? = null

	override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
		// Enter relevant functionality for when the first widget is created
		// fixme this approach has to be wrong.. need to check
		val j = SupervisorJob()
		job = j
		val scope = CoroutineScope(Dispatchers.IO + j)
		scope.launch {
			infoRepo.getByPos(settingsRepo.selectedPos).collect {
				when(it) {
					is Loading -> { /* todo show loading */
					}
					is Success -> updateAppWidget(context, appWidgetManager, appWidgetIds, it.data)
					is Error -> { /*todo show toast*/
					}
				}
			}
		}
	}

	override fun onDeleted(context: Context, appWidgetIds: IntArray) {
		// Enter relevant functionality for when the last widget is disabled
		job?.cancel()
	}

	private fun updateAppWidget(context: Context, awm: AppWidgetManager, ids: IntArray, info: WeatherInfo) {
		ids.forEach { id ->
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
				removeAllViews(R.id.ll_forecasts)
				// todo move formatters to util
				val timeFormatter = DateTimeFormatter.ofPattern("h a")
				val dayFormatter = DateTimeFormatter.ofPattern("EEE")
				// add 3 views for hourly (each 6 hrs), and 2 for daily
				val views = info.hourly.take(24).filterIndexed { i, _ -> i % 6 == 0 }.drop(1).map {
					val text = LocalDateTime.ofInstant(it.hour, info.location.zoneId).format(timeFormatter)
					getItemRemoteView(context, it.icon, text)
				} + info.daily.drop(1).map {
					val text = LocalDateTime.ofInstant(it.date, info.location.zoneId).format(dayFormatter)
					getItemRemoteView(context, it.icon, text)
				}
				for(v in views) {
					addView(R.id.ll_forecasts, v)
				}
			}
			// Instruct the widget manager to update the widget
			awm.updateAppWidget(id, views)
		}
	}

	private fun getItemRemoteView(context: Context, iconRes: Int, text: String) =
		RemoteViews(context.packageName, R.layout.item_widget_forecast).apply {
			setImageViewResource(R.id.iv_icon, iconRes)
			setTextViewText(R.id.tv_text, text)
		}
}

/*
internal fun broadcastUpdateToWidgets(appContext: Context) {
	val awm = AppWidgetManager.getInstance(appContext)
	val ids = awm.getAppWidgetIds(ComponentName(appContext, AppWidget::class.java))
	val intent = Intent().apply {
		action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
		putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
	}
	appContext.sendBroadcast(intent)
}*/
