package asceapps.weatheria.util

import android.content.Context
import androidx.core.content.ContextCompat

fun Context.getColors(vararg colorResId: Int) = IntArray(colorResId.size) {
	ContextCompat.getColor(this, colorResId[it])
}