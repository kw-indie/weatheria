package asceapps.weatheria.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.data.LocationEntity
import asceapps.weatheria.databinding.ItemSearchResultBinding

class SearchResultAdapter(
	private var mClickListener: OnLocationClickListener
): ListAdapter<LocationEntity, SearchResultAdapter.ViewHolder>(DiffCallback()) {

	init {
		stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(ItemSearchResultBinding.inflate(
			LayoutInflater.from(parent.context),
			parent, false
		).apply {clickListener = mClickListener})
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.bind(getItem(position))
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