package asceapps.weatheria.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.data.entity.SavedLocationEntity
import asceapps.weatheria.databinding.ItemLocationBinding

class LocationsAdapter(
	private val onDeleteClick: (SavedLocationEntity) -> Unit,
	private val onItemClick: (SavedLocationEntity) -> Unit,
	private val onHandleTouch: (ViewHolder) -> Unit
): BaseListAdapter<SavedLocationEntity, LocationsAdapter.ViewHolder>(DiffCallback()) {

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
		private val onDeleteClick: (SavedLocationEntity) -> Unit,
		private val onItemClick: (SavedLocationEntity) -> Unit,
		private val onHandleTouch: (ViewHolder) -> Unit
	): RecyclerView.ViewHolder(binding.root) {

		init {
			with(binding) {
				ibDelete.setOnClickListener {
					onDeleteClick(binding.location!!)
				}
				tvName.setOnClickListener {
					onItemClick(binding.location!!)
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

		fun bind(l: SavedLocationEntity) {
			with(binding) {
				location = l
				executePendingBindings()
			}
		}
	}

	private class DiffCallback: DiffUtil.ItemCallback<SavedLocationEntity>() {

		override fun areItemsTheSame(oldItem: SavedLocationEntity, newItem: SavedLocationEntity) =
			oldItem.id == newItem.id

		override fun areContentsTheSame(oldItem: SavedLocationEntity, newItem: SavedLocationEntity) =
			oldItem.id == newItem.id
	}
}