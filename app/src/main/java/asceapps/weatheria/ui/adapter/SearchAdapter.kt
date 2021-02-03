package asceapps.weatheria.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.data.entity.LocationEntity
import asceapps.weatheria.databinding.ItemSearchResultBinding

class SearchAdapter(
	private val onClick: (LocationEntity) -> Unit
): BaseListAdapter<LocationEntity, SearchAdapter.ViewHolder>(DiffCallback()) {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(ItemSearchResultBinding.inflate(
			LayoutInflater.from(parent.context),
			parent, false
		), onClick)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.bind(getItem(position))
	}

	override fun getItemId(position: Int): Long {
		return currentList[position].id.toLong()
	}

	class ViewHolder(
		private val binding: ItemSearchResultBinding,
		onClick: (LocationEntity) -> Unit
	): RecyclerView.ViewHolder(binding.root) {

		init {
			with(binding) {
				root.setOnClickListener {
					onClick(location!!) // had to add '!!' cuz of inferred type mismatch
				}
			}
		}

		fun bind(l: LocationEntity) {
			with(binding) {
				location = l
				executePendingBindings()
			}
		}
	}

	private class DiffCallback: DiffUtil.ItemCallback<LocationEntity>() {

		override fun areItemsTheSame(oldItem: LocationEntity, newItem: LocationEntity) =
			oldItem.id == newItem.id

		override fun areContentsTheSame(oldItem: LocationEntity, newItem: LocationEntity) =
			oldItem.id == newItem.id
	}
}