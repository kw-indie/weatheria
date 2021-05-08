package asceapps.weatheria.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.data.model.WeatherInfo
import asceapps.weatheria.databinding.ItemMyLocationsBinding
import java.util.*

class MyLocationsAdapter(
	private val itemCallback: ItemCallback
): RecyclerView.Adapter<MyLocationsAdapter.ViewHolder>() {

	private val touchHelper = ItemTouchHelper(ReorderCallback())
	val currentList get() = DiffCallback.list

	init {
		stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
	}

	override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
		touchHelper.attachToRecyclerView(recyclerView)
	}

	override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
		touchHelper.attachToRecyclerView(null)
	}

	override fun getItemCount() = currentList.size

	@SuppressLint("ClickableViewAccessibility")
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val holder: ViewHolder
		ItemMyLocationsBinding.inflate(LayoutInflater.from(parent.context), parent, false).apply {
			holder = ViewHolder(this)
			ibDelete.setOnClickListener { itemCallback.onDeleteClick(info!!) }
			tvName.setOnClickListener { itemCallback.onItemClick(holder.bindingAdapterPosition) }
			ivDragHandle.setOnTouchListener { _, e ->
				if(e.action == MotionEvent.ACTION_DOWN) {
					touchHelper.startDrag(holder)
					return@setOnTouchListener true
				}
				false
			}
		}
		return holder
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		with(holder.binding) {
			info = getItem(position)
			executePendingBindings()
		}
	}

	fun getItem(position: Int) = currentList[position]

	fun submitList(l: List<WeatherInfo>) {
		with(DiffCallback) {
			newList = l
			val diffResult = DiffUtil.calculateDiff(this)
			list.clear()
			list.addAll(l)
			diffResult.dispatchUpdatesTo(this@MyLocationsAdapter)
		}
	}

	fun reorder(fromPos: Int, toPos: Int) {
		if(fromPos == toPos) return
		if(fromPos < toPos) {
			Collections.rotate(currentList.subList(fromPos, toPos + 1), -1)
		} else {
			Collections.rotate(currentList.subList(toPos, fromPos + 1), 1)
		}
		notifyItemMoved(fromPos, toPos)
	}

	class ViewHolder(val binding: ItemMyLocationsBinding): RecyclerView.ViewHolder(binding.root)

	interface ItemCallback {

		fun onDeleteClick(info: WeatherInfo)
		fun onItemClick(pos: Int)
		fun onStartDrag(view: View)
		fun onEndDrag(view: View)
		fun onReorder(info: WeatherInfo, toPos: Int)
	}

	private object DiffCallback: DiffUtil.Callback() {

		val list = mutableListOf<WeatherInfo>()
		var newList = emptyList<WeatherInfo>()

		override fun getOldListSize() = list.size
		override fun getNewListSize() = newList.size
		override fun areItemsTheSame(oldPos: Int, newPos: Int) = list[oldPos].id == newList[newPos].id
		override fun areContentsTheSame(oldPos: Int, newPos: Int) = oldPos.hashCode() == newPos.hashCode()
	}

	private inner class ReorderCallback: ItemTouchHelper.SimpleCallback(
		ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
	) {

		private val noMove = 0 to 0
		private val debounce = 500
		private var lastMove = noMove
		private var lastMoveMillis = 0L

		override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
			super.onSelectedChanged(viewHolder, actionState)
			if(viewHolder == null) return
			if(actionState == ItemTouchHelper.ACTION_STATE_DRAG)
				itemCallback.onStartDrag(viewHolder.itemView)
		}

		override fun onMove(
			recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder
		): Boolean {
			val newMove = viewHolder.bindingAdapterPosition to target.bindingAdapterPosition
			if(lastMove != newMove && System.currentTimeMillis() > lastMoveMillis + debounce) {
				lastMove = newMove
				lastMoveMillis = System.currentTimeMillis()
				reorder(lastMove.first, lastMove.second)
				return true
			}
			return false
		}

		override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
			super.clearView(recyclerView, viewHolder)
			itemCallback.onEndDrag(viewHolder.itemView)
			val newPos = lastMove.second
			val info = getItem(newPos)
			itemCallback.onReorder(info, newPos)
			lastMove = noMove
		}

		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
	}
}