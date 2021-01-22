package asceapps.weatheria.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

abstract class BaseListAdapter<T, VH: RecyclerView.ViewHolder>(
	callback: DiffUtil.ItemCallback<T>
): ListAdapter<T, VH>(callback) {

	init {
		stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
		setHasStableIds(true)
		// todo diffUtil nullifies the need for stable ids
		// https://medium.com/androiddevelopers/merge-adapters-sequentially-with-mergeadapter-294d2942127a
	}

	final override fun setHasStableIds(hasStableIds: Boolean) {
		super.setHasStableIds(hasStableIds)
	}

	public override fun getItem(position: Int): T {
		return super.getItem(position)
	}

	abstract override fun getItemId(position: Int): Long
}