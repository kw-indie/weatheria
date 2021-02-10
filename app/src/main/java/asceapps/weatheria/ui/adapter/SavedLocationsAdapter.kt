package asceapps.weatheria.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.databinding.ItemLocationBinding
import asceapps.weatheria.model.Location
import java.util.*

class SavedLocationsAdapter(
	private val onDeleteClick: (Location) -> Unit,
	private val onItemClick: (Location) -> Unit,
	private val onStartDrag: (View) -> Unit,
	private val onEndDrag: (View) -> Unit,
	private val onReorder: (Location, Int) -> Unit
): BaseAdapter<Location, SavedLocationsAdapter.ViewHolder>(DiffCallback()) {

	private val touchHelper = ItemTouchHelper(ReorderCallback())

	override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
		touchHelper.attachToRecyclerView(recyclerView)
	}

	override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
		touchHelper.attachToRecyclerView(null)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(ItemLocationBinding.inflate(
			LayoutInflater.from(parent.context),
			parent, false
		), onDeleteClick, onItemClick, {touchHelper.startDrag(it)})
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.bind(getItem(position))
	}

	override fun getItemId(position: Int): Long {
		return list[position].id.toLong()
	}

	@SuppressLint("ClickableViewAccessibility")
	class ViewHolder(
		private val binding: ItemLocationBinding,
		onDeleteClick: (Location) -> Unit,
		onItemClick: (Location) -> Unit,
		onHandleTouch: (ViewHolder) -> Unit
	): RecyclerView.ViewHolder(binding.root) {

		init {
			// had to init here cuz i need a reference to this vh
			// and for consistency with other adapters
			with(binding) {
				ibDelete.setOnClickListener {
					onDeleteClick(location!!)
				}
				tvName.setOnClickListener {
					onItemClick(location!!)
				}
				ivDragHandle.setOnTouchListener {_, e ->
					if(e.action == MotionEvent.ACTION_DOWN) {
						onHandleTouch(this@ViewHolder)
						return@setOnTouchListener true
					}
					false
				}
			}
		}

		fun bind(l: Location) {
			with(binding) {
				location = l
				executePendingBindings()
			}
		}
	}

	private class DiffCallback: BaseAdapter.DiffCallback<Location>() {

		override fun areItemsTheSame(old: Location, new: Location) = old.id == new.id

		override fun areContentsTheSame(old: Location, new: Location) = true
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

		override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder): Boolean {
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