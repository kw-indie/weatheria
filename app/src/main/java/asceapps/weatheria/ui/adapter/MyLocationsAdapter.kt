package asceapps.weatheria.ui.adapter

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.R
import asceapps.weatheria.data.model.WeatherInfo
import asceapps.weatheria.databinding.ItemMyLocationsBinding
import java.util.*

class MyLocationsAdapter(
	private val itemCallback: ItemCallback
): BaseAdapter<WeatherInfo, ItemMyLocationsBinding>() {

	private val touchHelper = ItemTouchHelper(ReorderCallback())

	override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
		// since we want touch helper live as long as the adapter is attached,
		// we decided to not bother detach it and let it die with it
		touchHelper.attachToRecyclerView(recyclerView)
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onHolderCreated(holder: BindingHolder<ItemMyLocationsBinding>) {
		with(holder.binding) {
			ibDelete.setOnClickListener { itemCallback.onDeleteClick(item!!) }
			root.setOnClickListener { itemCallback.onItemClick(item!!.location.pos) }
			ivDragHandle.setOnTouchListener { _, e ->
				if(e.action == MotionEvent.ACTION_DOWN) {
					touchHelper.startDrag(holder)
					return@setOnTouchListener true
				}
				false
			}
		}
	}

	override fun getItemViewType(position: Int) = R.layout.item_my_locations

	interface ItemCallback {

		fun onDeleteClick(info: WeatherInfo)
		fun onItemClick(pos: Int)
		fun onReorder(info: WeatherInfo, toPos: Int)
	}

	private inner class ReorderCallback: ItemTouchHelper.SimpleCallback(
		ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
	) {

		private val noMove = 0 to 0
		private val debounce = 500
		private var lastMove = noMove
		private var nextMoveMinTime = 0L

		override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
			super.onSelectedChanged(viewHolder, actionState)
			//if(viewHolder == null) return // true for 'idle' state, false for 'drag' state
			if(actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
				// view dragging has started, play with it, eg. animate scale it, color it, etc
				val scale = 1.05f
				viewHolder!!.itemView.animate()
					.scaleX(scale)
					.scaleY(scale)
			}
		}

		override fun onMove(
			recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder
		): Boolean {
			val newMove = viewHolder.bindingAdapterPosition to target.bindingAdapterPosition
			val now = System.currentTimeMillis()
			if(lastMove != newMove && now > nextMoveMinTime) {
				lastMove = newMove
				nextMoveMinTime = now + debounce
				// do reorder in adapter list
				val snapshot = currentList.toMutableList()
				val (fromPos, toPos) = lastMove
				if(fromPos < toPos) {
					Collections.rotate(snapshot.subList(fromPos, toPos + 1), -1)
				} else {
					Collections.rotate(snapshot.subList(toPos, fromPos + 1), 1)
				}
				submitList(snapshot)
				return true
			}
			return false
		}

		override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
			super.clearView(recyclerView, viewHolder)
			// undo changes in onSelectedChanged
			val scale = 1f
			viewHolder.itemView.animate()
				.scaleX(scale)
				.scaleY(scale)
			// pass reorder to callback
			val newPos = lastMove.second
			val info = getItem(newPos)
			//val oldPos = info.location.pos
			//if(oldPos != newPos) // there is already an internal check for equality
			itemCallback.onReorder(info, newPos)
			lastMove = noMove
		}

		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
	}
}