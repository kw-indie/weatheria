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
import asceapps.weatheria.data.repo.SettingsRepo
import asceapps.weatheria.shared.data.model.WeatherInfo
import asceapps.weatheria.shared.data.repo.ACCURACY_OUTDATED
import asceapps.weatheria.shared.data.repo.Loading
import asceapps.weatheria.shared.data.repo.Success
import asceapps.weatheria.shared.data.repo.WeatherInfoRepo
import asceapps.weatheria.ui.MainActivity
import asceapps.weatheria.util.Formatter
import asceapps.weatheria.util.IconMapper
import asceapps.weatheria.worker.RefreshWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
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
			infoRepo.getByPos(settingsRepo.selectedPos) // todo react to selectedPos and unit sys change
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
			if(info.accuracy == ACCURACY_OUTDATED) {
				// todo show warning/remove views
			} else {
				val current = info.current
				setTextViewText(R.id.tv_location, info.location.name)
				setTextViewText(R.id.tv_temp, Formatter.temp(current.temp))
				setTextViewText(R.id.tv_humidity_value, Formatter.percent(current.humidity))
				val uvString = context.getString(
					R.string.f_2s_p,
					Formatter.number(current.uv),
					context.resources.getStringArray(R.array.uv_levels)[Formatter.uvLevel(current.uv)]
				)
				setTextViewText(R.id.tv_uv_value, uvString)
				setImageViewResource(R.id.iv_icon, IconMapper[current.iconIndex])
				setOnClickPendingIntent(R.id.iv_refresh, getUpdateDataBroadcastIntent(context))
				val lastUpdate = context.getString(R.string.f_last_update, Formatter.relativeTime(info.lastUpdate))
				setTextViewText(R.id.tv_last_update, lastUpdate)
				removeAllViews(R.id.ll_forecasts)
				// add 3 views for hourly, 1 divider, and 3 for daily
				val items = ArrayList<RemoteViews>(7)
				val thisHour = info.thisHourOrNull
				// todo take best 6
				info.hourly.dropWhile { it != thisHour }
					.filterIndexed { i, _ -> i % 4 == 0 } // take 1 every 4 hours
					.take(3)
					.mapTo(items) {
						val text = Formatter.zonedHour(it.hour, info.location.zoneId)
						getItemRemoteView(context, IconMapper[it.iconIndex], text)
					}
				items.add(RemoteViews(context.packageName, R.layout.item_app_widget_forecast_divider))
				// todo take best 3
				val today = info.todayOrNull
				info.daily.dropWhile { it != today }
					.take(3)
					.mapTo(items) {
						val text = Formatter.zonedDay(it.date, info.location.zoneId)
						getItemRemoteView(context, IconMapper[it.dayIconIndex], text) // todo use both icons
					}
				for(i in items) {
					addView(R.id.ll_forecasts, i)
				}
				setOnClickPendingIntent(R.id.root, getOpenAppIntent(context))
			}
		}
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
		RemoteViews(context.packageName, R.layout.item_app_widget_forecast).apply {
			setImageViewResource(R.id.iv_icon, iconRes)
			setTextViewText(R.id.tv_text, text)
		}
}
