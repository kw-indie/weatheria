package asceapps.weatheria.ui.adapter

import android.graphics.Rect
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import asceapps.weatheria.data.api.WeatherApi
import asceapps.weatheria.drawable.DirectionDrawable
import coil.load

@BindingAdapter("icon")
fun loadIconFromUrl(view: ImageView, icon: String) {
	view.load(WeatherApi.ICON_URL_FORMAT.format(icon)) {
		// placeholder(res_id)
		// error(res_id)
		// transformations(CircleCropTransformation())
		crossfade(true)
	}
}

@BindingAdapter("windDirection")
fun addWindDirectionDrawable(view: TextView, deg: Int) {
	val compDs = view.compoundDrawablesRelative
	val d = compDs[2] as? DirectionDrawable ?: DirectionDrawable().apply {
		val size = compDs[0].intrinsicHeight
		bounds = Rect(0, 0, size, size)
		view.setCompoundDrawablesRelative(compDs[0], null, this, null)
	}
	d.apply {
		colorFilter = compDs[0].colorFilter
		rotation = deg.toFloat()
	}
	view.invalidateDrawable(d)
}