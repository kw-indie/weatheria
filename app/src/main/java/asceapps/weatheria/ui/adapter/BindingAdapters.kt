package asceapps.weatheria.ui.adapter

import android.graphics.Rect
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.data.api.WeatherApi
import asceapps.weatheria.ui.drawable.DirectionDrawable
import coil.load

@BindingAdapter("dividerEnabled")
fun enabledDivider(recyclerView: RecyclerView, enabled: Boolean) {
	if(enabled) { // meaningless.. one only needs to add this attr if they want this to be true
		with(recyclerView) {
			val layoutManager = layoutManager as LinearLayoutManager
			val divider = DividerItemDecoration(context, layoutManager.orientation)
			addItemDecoration(divider)
		}
	}
}

@BindingAdapter("hasFixedSize")
fun setHasFixedSize(recyclerView: RecyclerView, hasFixedSize: Boolean) {
	recyclerView.setHasFixedSize(hasFixedSize)
}

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