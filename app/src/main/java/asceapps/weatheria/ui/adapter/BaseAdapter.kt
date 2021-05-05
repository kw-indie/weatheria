package asceapps.weatheria.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.data.base.IDed

abstract class BaseAdapter<T: IDed, VH: RecyclerView.ViewHolder>:
	ListAdapter<T, VH>(IDedItemCallback()) {

	init {
		stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
	}

	public override fun getItem(position: Int): T {
		return super.getItem(position)
	}

	private class IDedItemCallback<T: IDed>: DiffUtil.ItemCallback<T>() {

		override fun areItemsTheSame(oldT: T, newT: T) = oldT.id == newT.id
		override fun areContentsTheSame(oldT: T, newT: T) = oldT.hashCode() == newT.hashCode()
	}
}