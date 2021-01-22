package asceapps.weatheria.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.data.entity.LocationEntity
import asceapps.weatheria.databinding.ItemSearchResultBinding

class SearchAdapter(
	private var onClick: (LocationEntity) -> Unit
): BaseListAdapter<LocationEntity, SearchAdapter.ViewHolder>(DiffCallback()) {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(ItemSearchResultBinding.inflate(
			LayoutInflater.from(parent.context),
			parent, false
		).apply {
			root.setOnClickListener {
				onClick(location!!) // fixme had to add '!!' cuz of inferred type mismatch
			}
		})
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.bind(getItem(position))
	}

	override fun getItemId(position: Int): Long {
		return currentList[position].id.toLong()
	}

	class ViewHolder(private val binding: ItemSearchResultBinding): RecyclerView.ViewHolder(binding.root) {

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