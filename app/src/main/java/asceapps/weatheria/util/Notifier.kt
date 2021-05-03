package asceapps.weatheria.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import asceapps.weatheria.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Notifier @Inject constructor(@ApplicationContext context: Context) {

	private val channelId = "Default"

	init {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val notificationManager =
				context.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
			val existingChannel = notificationManager.getNotificationChannel(channelId)
			if(existingChannel == null) {
				// Create the NotificationChannel
				val name = context.getString(R.string.noti_def_channel_name)
				val importance = NotificationManager.IMPORTANCE_DEFAULT
				val channel = NotificationChannel(channelId, name, importance)
				channel.description = context.getString(R.string.noti_def_channel_desc)
				notificationManager.createNotificationChannel(channel)
			}
		}
	}

	fun postNotification(
		id: Long,
		context: Context,
		intent: PendingIntent,
		titleResId: Int,
		smallIconResId: Int,
		textResId: Int
	) {
		val builder = NotificationCompat.Builder(context, channelId)
		builder.setContentTitle(context.getString(titleResId))
			.setSmallIcon(smallIconResId)
		val text = context.getString(textResId)
		val notification = builder.setContentText(text)
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setContentIntent(intent)
			.setAutoCancel(true)
			.build()
		val notificationManager = NotificationManagerCompat.from(context)
		// Remove prior notifications; only allow one at a time to edit the latest item
		notificationManager.cancelAll()
		notificationManager.notify(id.toInt(), notification)
	}
}