package asceapps.weatheria.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.data.model.FoundLocation
import asceapps.weatheria.databinding.ItemAddLocationBinding

class AddLocationAdapter(
	private val onItemClick: (FoundLocation) -> Unit
): BaseAdapter<FoundLocation, AddLocationAdapter.ViewHolder>() {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val binding = ItemAddLocationBinding.inflate(
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
		with(holder.binding) {
			location = getItem(position)
			executePendingBindings()
		}
	}

	class ViewHolder(val binding: ItemAddLocationBinding): RecyclerView.ViewHolder(binding.root)
}