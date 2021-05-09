package asceapps.weatheria.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import asceapps.weatheria.data.model.FoundLocation
import asceapps.weatheria.databinding.ItemAddLocationBinding

class AddLocationAdapter(
	private val onItemClick: (FoundLocation) -> Unit
): BaseAdapter<FoundLocation>() {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingHolder {
		val binding = ItemAddLocationBinding.inflate(
			LayoutInflater.from(parent.context),
			parent,
			false
		).apply {
			root.setOnClickListener {
				onItemClick(item!!)
			}
		}
		return BindingHolder(binding)
	}
}