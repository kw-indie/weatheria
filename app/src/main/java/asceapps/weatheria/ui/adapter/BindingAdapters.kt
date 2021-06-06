package asceapps.weatheria.ui.adapter

import android.graphics.Rect
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.R
import asceapps.weatheria.ui.drawable.DirectionDrawable

// - can have multiple attr in 1 method like:
// @BindingAdapter(values = [attr1, attr2, ..], requireAll = Boolean)
// funName(v: View, attr1: Type1, attr2: Type2, ..)
// don't abuse it. make sure an adapter has 1 responsibility
// - classes with setters don't need binding adapters
// - if these were not top level fun's, we need to add @JvmStatic
@BindingAdapter("hasDividers")
fun addDividers(recyclerView: RecyclerView, hasDividers: Boolean) {
	if(hasDividers) {
		with(recyclerView) {
			val layoutManager = layoutManager as LinearLayoutManager
			val divider = DividerItemDecoration(context, layoutManager.orientation)
			addItemDecoration(divider)
		}
	}
}

@BindingAdapter("icon")
fun setIcon(view: ImageView, icon: String) {
	val condition = icon.substring(0, 4).toInt()
	val d = 'd'
	val n = 'n'
	val pod = icon.getOrNull(4)
	val resId = when(condition) {
		1000 -> if(pod == d) R.drawable.w_clear_d else R.drawable.w_clear_n
		1003 -> if(pod == d) R.drawable.w_cloudy_p_d else R.drawable.w_cloudy_p_n
		1006 -> if(pod == d) R.drawable.w_cloudy_d else R.drawable.w_cloudy_n
		1009 -> R.drawable.w_overcast
		1030 -> R.drawable.w_mist
		1063, 1180, 1183, 1186, 1189, 1240 -> when(pod) {
			d -> R.drawable.w_rain_l_d
			n -> R.drawable.w_rain_l_n
			else -> R.drawable.w_rain_l
		}
		1066, 1210, 1213, 1216, 1219, 1255 -> when(pod) {
			d -> R.drawable.w_snow_l_d
			n -> R.drawable.w_snow_l_n
			else -> R.drawable.w_snow_l
		}
		1069, 1204, 1207, 1249 -> when(pod) {
			d -> R.drawable.w_sleet_l_d
			n -> R.drawable.w_sleet_l_n
			else -> R.drawable.w_sleet_l
		}
		1072, 1168 -> R.drawable.w_drizzle_l_f
		1087, 1273, 1276 -> when(pod) {
			d -> R.drawable.w_thunder_d
			n -> R.drawable.w_thunder_n
			else -> R.drawable.w_thunder
		}
		1114 -> R.drawable.w_snow_b
		1117 -> R.drawable.w_blizzard
		1135 -> R.drawable.w_fog
		1147 -> R.drawable.w_fog_f
		1150, 1153 -> R.drawable.w_drizzle_l
		1171 -> R.drawable.w_drizzle_h_f
		1192, 1195, 1243 -> when(pod) {
			d -> R.drawable.w_rain_h_d
			n -> R.drawable.w_rain_h_n
			else -> R.drawable.w_rain_h
		}
		1198, 1201 -> R.drawable.w_rain_f
		1222, 1225, 1258 -> when(pod) {
			d -> R.drawable.w_snow_h_d
			n -> R.drawable.w_snow_h_n
			else -> R.drawable.w_snow_h
		}
		1237, 1261 -> when(pod) {
			d -> R.drawable.w_ice_pellets_l_d
			n -> R.drawable.w_ice_pellets_l_n
			else -> R.drawable.w_ice_pellets_l
		}
		1246 -> if(pod == d) R.drawable.w_rain_t_d else R.drawable.w_rain_t_n
		1252 -> if(pod == d) R.drawable.w_sleet_h_d else R.drawable.w_sleet_h_n
		1264 -> if(pod == d) R.drawable.w_ice_pellets_h_d else R.drawable.w_ice_pellets_h_n
		1279, 1282 -> when(pod) {
			d -> R.drawable.w_snow_thunder_d
			n -> R.drawable.w_snow_thunder_n
			else -> R.drawable.w_snow_thunder
		}
		else -> throw IllegalArgumentException()
	}
	view.setImageResource(resId)
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