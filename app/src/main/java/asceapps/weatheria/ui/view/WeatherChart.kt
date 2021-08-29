package asceapps.weatheria.ui.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.withStyledAttributes
import androidx.core.view.get
import asceapps.weatheria.R
import asceapps.weatheria.databinding.ItemChartBinding
import asceapps.weatheria.shared.data.model.WeatherInfo
import asceapps.weatheria.shared.data.model.zonedDay
import asceapps.weatheria.shared.data.model.zonedHour
import asceapps.weatheria.shared.data.repo.today

class WeatherChart @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
): LinearLayoutCompat(context, attrs, defStyleAttr) {

	private class Item(val time: String, val imgResId: Int, val pop: Int, val v1: Int, val v2: Int)

	private var items = emptyList<Item>()
	private val paths = mutableListOf<Path>()
	private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.STROKE
	}
	private val points = mutableListOf<PointF>()
	private val textPoints = mutableListOf<PointF>()
	private val pointsPaint = Paint(Paint.ANTI_ALIAS_FLAG)
	var lineColor: Int
		get() = pathPaint.color
		set(value) {
			if(pathPaint.color != value) {
				pathPaint.color = value
				pointsPaint.color = value
				invalidate()
			}
		}
	private val dp = resources.displayMetrics.density
	var pointTextSize: Float
		get() = pointsPaint.textSize
		set(value) {
			if(pointsPaint.textSize != value) {
				pointsPaint.textSize = value
				invalidate()
			}
		}
	var pointRadius = 0f
		set(value) {
			if(field != value) {
				field = value
				invalidate()
			}
		}
	var graphPadding = 0f
		set(value) {
			if(field != value) {
				field = value
				measurePathAndInvalidate()
			}
		}
	private var dataType = 0

	companion object {
		const val HOURLY = 0
		const val DAILY = 1
	}

	init {
		context.withStyledAttributes(attrs, R.styleable.WeatherChart, defStyleAttr, 0) {
			val lineColor = getColor(R.styleable.WeatherChart_lineColor, Color.WHITE)
			pathPaint.apply {
				color = lineColor
				strokeWidth = getDimension(R.styleable.WeatherChart_lineWidth, 1f)
			}
			pointsPaint.apply {
				textSize = getDimension(R.styleable.WeatherChart_pointTextSize, dp * 12)
				color = lineColor
			}
			pointRadius = getDimension(R.styleable.WeatherChart_pointRadius, dp * 2)
			graphPadding = getDimension(R.styleable.WeatherChart_graphPadding, dp * 12)
			dataType = getInt(R.styleable.WeatherChart_dataType, HOURLY)
		}

		if(isInEditMode) {
			val sampleHours = listOf("4 AM", "8 AM", "12 PM", "4 PM", "8 PM", "12 AM")
			val sampleDays = listOf("Today", "Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")
			val sampleIcons = listOf(
				R.drawable.w_fog,
				R.drawable.w_clear_d,
				R.drawable.w_cloudy_p_d,
				R.drawable.w_rain_h_d,
				R.drawable.w_snow_thunder,
				R.drawable.w_sleet_h_d
			)
			val popRange = 0..100
			val hiTempRange = 15..30
			val loTempRange = 5..20
			val tempRange = hiTempRange + loTempRange
			items = if(dataType == HOURLY) {
				sampleHours.mapIndexed { i, h ->
					Item(h, sampleIcons[i], popRange.random(), tempRange.random(), 0)
				}
			} else {
				val start = (0..5).random()
				sampleDays.subList(start, start + 3).mapIndexed { i, d ->
					Item(d, sampleIcons[i], popRange.random(), loTempRange.random(), hiTempRange.random())
				}
			}
			addChildren()
		}
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		measurePathAndInvalidate()
	}

	fun setInfo(info: WeatherInfo, dataType: Int) {
		this.dataType = dataType
		items = if(dataType == HOURLY) {
			info.hourly.take(24).filterIndexed { i, _ -> i % 4 == 0 }.map {
				Item(
					zonedHour(it.hour, info.location.zoneId).uppercase(),
					it.iconResId,
					it.pop.v,
					it.temp.v,
					0
				)
			}
		} else {
			val today = today(info.daily)
			info.daily.map {
				val day = if(it == today) resources.getString(R.string.today)
				else zonedDay(it.date, info.location.zoneId)
				Item(
					day.uppercase(),
					it.dayIconResId,
					it.pop.v,
					it.min.v,
					it.max.v
				)
			}
		}
		addChildren()
		if(isLaidOut) {
			measurePathAndInvalidate()
		}
	}

	private fun addChildren() {
		removeAllViews()
		val inflater = LayoutInflater.from(context)
		items.forEach {
			val view = ItemChartBinding.inflate(inflater, this, false).apply {
				tvTime.text = it.time
				ivIcon.setImageResource(it.imgResId)
				tvPop.text = it.pop.toString()
			}.root
			addView(view)
		}
	}

	private fun measurePathAndInvalidate() {
		if(items.isEmpty()) return
		paths.clear()
		points.clear()
		textPoints.clear()
		val wOf1 = width / items.size
		val xOffset = paddingStart + wOf1 / 2f
		val yOffset = get(0).measuredHeight + graphPadding // min in value, highest in graph
		val graphHeight = height - graphPadding - yOffset
		if(dataType == HOURLY) {
			val path = Path()
			paths.add(path)
			val values = items.map { it.v1 }
			val min = values.minOrNull()!!
			val max = values.maxOrNull()!!
			setupPath(wOf1, xOffset, yOffset, graphHeight, path, values, min, max)
		} else {
			val p1 = Path()
			val p2 = Path()
			paths.add(p1)
			paths.add(p2)
			val minValues = items.map { it.v1 }
			val maxValues = items.map { it.v2 }
			val min = minValues.minOrNull()!!
			val max = maxValues.maxOrNull()!!
			setupPath(wOf1, xOffset, yOffset, graphHeight, p1, minValues, min, max)
			setupPath(wOf1, xOffset, yOffset, graphHeight, p2, maxValues, min, max)
		}

		invalidate()
	}

	private fun setupPath(
		wOf1: Int, xOffset: Float, yOffset: Float, graphH: Float, path: Path,
		values: List<Int>, minVal: Int, maxVal: Int
	) {
		val valRange = (maxVal - minVal).toFloat()
		val textYOffset = pointRadius * 5
		for(i in values.indices) {
			val v = values[i]
			val x = xOffset + wOf1 * i
			val y = yOffset + (maxVal - v) / valRange * graphH
			if(i == 0) path.moveTo(x, y)
			else path.lineTo(x, y)
			points.add(PointF(x, y))
			val textW = pointsPaint.measureText(v.toString())
			textPoints.add(PointF(x - textW / 2, y - textYOffset))
		}
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)

		pathPaint.alpha = 127
		paths.forEach {
			canvas.drawPath(it, pathPaint)
		}
		points.forEach {
			canvas.drawCircle(it.x, it.y, pointRadius, pointsPaint)
		}
		if(dataType == HOURLY) {
			items.forEachIndexed { i, item ->
				val p = textPoints[i]
				canvas.drawText(item.v1.toString(), p.x, p.y, pointsPaint)
			}
		} else {
			val half = textPoints.size / 2
			items.forEachIndexed { i, item ->
				val pMin = textPoints[i]
				val pMax = textPoints[i + half]
				canvas.drawText(item.v1.toString(), pMin.x, pMin.y, pointsPaint)
				canvas.drawText(item.v2.toString(), pMax.x, pMax.y, pointsPaint)
			}
		}
	}
}