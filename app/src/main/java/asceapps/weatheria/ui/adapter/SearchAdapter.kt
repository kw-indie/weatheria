package asceapps.weatheria.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.data.api.FindResponse
import asceapps.weatheria.databinding.ItemSearchResultBinding

class SearchAdapter(
	private val onItemClick: (FindResponse.Location) -> Unit
) : BaseAdapter<FindResponse.Location, SearchAdapter.ViewHolder>() {

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

		fun bind(l: FindResponse.Location) {
			with(binding) {
				location = l
				executePendingBindings()
			}
		}
	}
}