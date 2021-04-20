package asceapps.weatheria.drawable

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

class DirectionDrawable: Drawable() {

	var rotation: Float = 0f

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.WHITE
	}
	private val path = Path().apply {
		moveTo(20f, 12f)
		lineTo(4.6f, 7f)
		lineTo(4f, 7.475f)
		lineTo(6.526f, 12f)
		lineTo(4f, 16.526f)
		lineTo(4.6f, 17f)
		close()
	}

	override fun draw(canvas: Canvas) {
		val w = bounds.width()
		val h = bounds.height()
		val cx = w / 2f
		val cy = h / 2f
		val sx = w / 24f
		val sy = h / 24f

		canvas.run {
			save()
			rotate(-rotation, cx, cy)
			// scale so our path points are correct regardless of drawable size
			scale(sx, sy)
			drawPath(path, paint)
			restore()
		}
	}

	override fun setAlpha(alpha: Int) {
		paint.alpha = alpha
		invalidateSelf()
	}

	override fun setColorFilter(colorFilter: ColorFilter?) {
		paint.colorFilter = colorFilter
		invalidateSelf()
	}

	override fun getOpacity() = PixelFormat.TRANSLUCENT
}
