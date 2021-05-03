package asceapps.weatheria.ui.adapter

import android.graphics.Rect
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.databinding.BindingAdapter
import asceapps.weatheria.data.api.WeatherApi
import asceapps.weatheria.ui.drawable.DirectionDrawable
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
fun setWindDirectionDrawable(tv: TextView, deg: Int) {
	val compDs = tv.compoundDrawablesRelative
	val d = compDs[2] as? DirectionDrawable ?: DirectionDrawable().apply {
		val size = compDs[0].intrinsicHeight
		bounds = Rect(0, 0, size, size)
		setTintList(TextViewCompat.getCompoundDrawableTintList(tv))
		tv.setCompoundDrawablesRelative(compDs[0], null, this, null)
	}
	d.deg = deg
}