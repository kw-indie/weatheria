package asceapps.weatheria.util

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import asceapps.weatheria.api.WeatherService
import coil.load

@BindingAdapter("icon")
fun loadIconFromUrl(view: ImageView, icon: String) {
	view.load(WeatherService.ICON_URL_PATTERN.format(icon)) {
		// placeholder(res_id)
		// error(res_id)
		// transformations(CircleCropTransformation())
		crossfade(true)
	}
}