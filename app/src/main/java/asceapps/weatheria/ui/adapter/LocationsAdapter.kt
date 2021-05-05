package asceapps.weatheria.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.data.base.IDed
import asceapps.weatheria.data.model.WeatherInfo
import asceapps.weatheria.databinding.ItemLocationBinding
import java.util.*

class LocationsAdapter(
	private val onDeleteClick: (WeatherInfo) -> Unit,
	private val onItemClick: (WeatherInfo, Int) -> Unit,
	private val onStartDrag: (View) -> Unit,
	private val onEndDrag: (View) -> Unit,
	private val onReorder: (WeatherInfo, Int) -> Unit
): RecyclerView.Adapter<LocationsAdapter.ViewHolder>() {

	private val list = mutableListOf<WeatherInfo>()
	private val callback = HashCallback(list)
	private val touchHelper = ItemTouchHelper(ReorderCallback())

	init {
		stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
	}

	override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
		touchHelper.attachToRecyclerView(recyclerView)
	}

	override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
		touchHelper.attachToRecyclerView(null)
	}

	fun getItem(position: Int) = list[position]

	override fun getItemCount() = list.size

	fun reorder(fromPos: Int, toPos: Int) {
		if(fromPos == toPos) return
		if(fromPos < toPos) {
			Collections.rotate(list.subList(fromPos, toPos + 1), -1)
		} else {
			Collections.rotate(list.subList(toPos, fromPos + 1), 1)
		}
		notifyItemMoved(fromPos, toPos)
	}

	fun submitList(newList: List<WeatherInfo>) {
		callback.newList = newList
		val diffResult = DiffUtil.calculateDiff(callback)
		list.clear()
		list.addAll(newList)
		diffResult.dispatchUpdatesTo(this)
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val holder: ViewHolder
		ItemLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false).apply {
			holder = ViewHolder(this)
			ibDelete.setOnClickListener { onDeleteClick(info!!) }
			tvName.setOnClickListener { onItemClick(info!!, holder.bindingAdapterPosition) }
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
		holder.bind(getItem(position))
	}

	class ViewHolder(private val binding: ItemLocationBinding): RecyclerView.ViewHolder(binding.root) {

		fun bind(i: WeatherInfo) {
			with(binding) {
				info = i
				executePendingBindings()
			}
		}
	}

	class HashCallback<T: IDed>(private val list: List<T>): DiffUtil.Callback() {

		var newList: List<T> = emptyList()

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
			if(actionState == ItemTouchHelper.ACTION_STATE_DRAG) onStartDrag(viewHolder.itemView)
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
			onEndDrag(viewHolder.itemView)
			val newPos = lastMove.second
			val item = getItem(newPos)
			onReorder(item, newPos)
			lastMove = noMove
		}

		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
	}
}