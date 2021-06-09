package asceapps.weatheria.ui.adapter

import android.graphics.Rect
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.ui.drawable.DirectionDrawable

@BindingAdapter("goneIf")
fun hideView(v: View, b: Boolean) {
	v.isVisible = !b
}

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

// this is needed cuz stupid dataBinding doesn't recognize input id as reference type, but as int color
@BindingAdapter("icon")
fun setIcon(iv: ImageView, resId: Int) {
	iv.setImageResource(resId)
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