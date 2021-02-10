package asceapps.weatheria.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.data.entity.LocationEntity
import asceapps.weatheria.databinding.ItemSearchResultBinding

class SearchAdapter(
	private val onClick: (LocationEntity) -> Unit
): BaseAdapter<LocationEntity, SearchAdapter.ViewHolder>(DiffCallback()) {

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
		return list[position].id.toLong()
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

	private class DiffCallback: BaseAdapter.DiffCallback<LocationEntity>() {

		override fun areItemsTheSame(old: LocationEntity, new: LocationEntity) = old.id == new.id

		override fun areContentsTheSame(old: LocationEntity, new: LocationEntity) = true
	}
}