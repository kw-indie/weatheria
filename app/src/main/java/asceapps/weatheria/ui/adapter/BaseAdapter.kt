package asceapps.weatheria.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.util.*

abstract class BaseAdapter<T, VH: RecyclerView.ViewHolder>(
	private val callback: DiffCallback<T>
): RecyclerView.Adapter<VH>() {

	protected val list = mutableListOf<T>()

	init {
		stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
		// todo diffUtil removes the need for stable ids
		// https://medium.com/androiddevelopers/merge-adapters-sequentially-with-mergeadapter-294d2942127a
		setHasStableIds(true)

		callback.oldList = list
	}

	final override fun setHasStableIds(hasStableIds: Boolean) {
		super.setHasStableIds(hasStableIds)
	}

	override fun getItemCount() = list.size

	abstract override fun getItemId(position: Int): Long

	open fun getItem(position: Int) = list[position]

	fun reorder(fromPos: Int, toPos: Int) {
		if(fromPos == toPos) return
		if(fromPos < toPos) {
			Collections.rotate(list.subList(fromPos, toPos + 1), -1)
		} else {
			Collections.rotate(list.subList(toPos, fromPos + 1), 1)
		}
		notifyItemMoved(fromPos, toPos)
	}

	open fun submitList(newList: List<T>) {
		callback.newList = newList
		val diffResult = DiffUtil.calculateDiff(callback)
		list.clear()
		list.addAll(newList)
		diffResult.dispatchUpdatesTo(this)
	}

	abstract class DiffCallback<T>: DiffUtil.Callback() {

		var oldList: List<T> = emptyList()
		var newList: List<T> = emptyList()

		final override fun getOldListSize() = oldList.size
		final override fun getNewListSize() = newList.size
		final override fun areItemsTheSame(oldPos: Int, newPos: Int) =
			areItemsTheSame(oldList[oldPos], newList[newPos])

		final override fun areContentsTheSame(oldPos: Int, newPos: Int) =
			areContentsTheSame(oldList[oldPos], newList[newPos])

		abstract fun areItemsTheSame(old: T, new: T): Boolean
		abstract fun areContentsTheSame(old: T, new: T): Boolean
	}
}