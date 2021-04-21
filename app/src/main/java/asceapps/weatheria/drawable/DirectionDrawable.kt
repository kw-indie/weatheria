package asceapps.weatheria.drawable

import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable

// todo lots of additions need to be made to make this fully integrated drawable like any other, including:
//  - support for api 24+
//  - custom-view-like xml (styleable attrs, etc.)
//  - drawable specific overrides (look at ColorDrawable & VectorDrawable for inspiration)
class DirectionDrawable: Drawable() {

	companion object {

		private const val DEFAULT_COLOR = Color.BLACK
		private val PATH = Path().apply {
			moveTo(8f, 12f)
			lineTo(5f, 6f)
			lineTo(19f, 12f)
			lineTo(5f, 18f)
			close()
		}
	}

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = DEFAULT_COLOR
		style = Paint.Style.STROKE
		strokeWidth = 2.4f
	}
	private val path = Path().apply { set(PATH) }

	var deg = 0
		set(value) {
			if(field != value) {
				field = value
				updatePath()
			}
		}

	override fun onBoundsChange(bounds: Rect) {
		updatePath()
	}

	private fun updatePath() {
		path.rewind()
		val c = 12f
		val size = 24f
		val sx = bounds.right / size
		val sy = bounds.bottom / size
		Matrix().apply {
			setRotate(deg.toFloat(), c, c)
			postScale(sx, sy)
			PATH.transform(this, path)
		}
		invalidateSelf()
	}

	override fun draw(canvas: Canvas) {
		canvas.drawPath(path, paint)
	}

	override fun setAlpha(alpha: Int) {
		if(paint.alpha != alpha) {
			paint.alpha = alpha
			invalidateSelf()
		}
	}

	override fun getAlpha() = paint.alpha

	override fun setColorFilter(colorFilter: ColorFilter?) {
		paint.colorFilter = colorFilter
	}

	override fun getColorFilter(): ColorFilter? = paint.colorFilter

	override fun getOpacity() = PixelFormat.TRANSLUCENT

	override fun setTintList(tint: ColorStateList?) {
		val newColor = tint?.defaultColor ?: DEFAULT_COLOR
		if(paint.color != newColor) {
			paint.color = newColor
			invalidateSelf()
		}
	}
}
