package asceapps.weatheria.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.databinding.ItemLocationBinding
import asceapps.weatheria.model.Location

class LocationsAdapter(
	private val onDeleteClick: (Location) -> Unit,
	private val onItemClick: (Location) -> Unit,
	private val onHandleTouch: (ViewHolder) -> Unit
): BaseListAdapter<Location, LocationsAdapter.ViewHolder>(DiffCallback()) {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(ItemLocationBinding.inflate(
			LayoutInflater.from(parent.context),
			parent, false
		), onDeleteClick, onItemClick, onHandleTouch)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.bind(getItem(position))
	}

	override fun getItemId(position: Int): Long {
		return currentList[position].id.toLong()
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

	private class DiffCallback: DiffUtil.ItemCallback<Location>() {

		override fun areItemsTheSame(oldItem: Location, newItem: Location) =
			oldItem.id == newItem.id

		override fun areContentsTheSame(oldItem: Location, newItem: Location) =
			oldItem.id == newItem.id
	}
}