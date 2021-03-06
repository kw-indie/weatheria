package asceapps.weatheria.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.data.entity.LocationEntity
import asceapps.weatheria.databinding.ItemSearchResultBinding

class SearchAdapter(
	private val onItemClick: (LocationEntity) -> Unit
) : BaseAdapter<LocationEntity, SearchAdapter.ViewHolder>() {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val binding = ItemSearchResultBinding.inflate(
			LayoutInflater.from(parent.context),
			parent,
			false
		).apply {
			root.setOnClickListener {
				onItemClick(location!!)
			}
		}
		return ViewHolder(binding)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.bind(getItem(position))
	}

	class ViewHolder(
		private val binding: ItemSearchResultBinding
	) : RecyclerView.ViewHolder(binding.root) {

		fun bind(l: LocationEntity) {
			with(binding) {
				location = l
				executePendingBindings()
			}
		}
	}
}