package asceapps.weatheria.ui.drawable

import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.graphics.PathParser

// todo lots of additions need to be made to make this fully integrated drawable like any other, including:
//  - api 24+ allows use of custom drawable in xml
//  - custom-view-like xml (styleable attrs, etc.)
//  - drawable specific overrides (look at ColorDrawable & VectorDrawable for inspiration)
class DirectionDrawable: Drawable() {

	companion object {

		private const val COLOR = Color.BLACK
		private const val SIZE = 24f
		private val PATH = PathParser.createPathFromPathData(
			"M5,7l14,5-14,5 3-5z"
		)
	}

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		// default color is black
		style = Paint.Style.STROKE
		strokeWidth = 2.4f
	}
	private val path = Path(PATH)

	var deg = 0
		set(value) {
			if(field != value) {
				field = value
				setup()
			}
		}

	override fun onBoundsChange(bounds: Rect) {
		setup()
	}

	private fun setup() {
		path.rewind()
		val c = SIZE / 2
		val sx = bounds.right / SIZE
		val sy = bounds.bottom / SIZE
		Matrix().apply {
			setRotate(-deg.toFloat(), c, c)
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
		if(paint.colorFilter != colorFilter) {
			paint.colorFilter = colorFilter
			invalidateSelf()
		}
	}

	override fun getColorFilter(): ColorFilter? = paint.colorFilter

	override fun getOpacity() = PixelFormat.TRANSLUCENT

	// for integration with ui theme
	override fun setTintList(tint: ColorStateList?) {
		val newColor = tint?.defaultColor ?: COLOR
		if(paint.color != newColor) {
			paint.color = newColor
			invalidateSelf()
		}
	}
}
