package asceapps.weatheria.ui.adapter

import asceapps.weatheria.R
import asceapps.weatheria.data.model.FoundLocation
import asceapps.weatheria.databinding.ItemAddLocationBinding

class AddLocationAdapter(
	private val onItemClick: (FoundLocation) -> Unit
): BaseAdapter<FoundLocation, ItemAddLocationBinding>() {

	override fun onHolderCreated(holder: BindingHolder<ItemAddLocationBinding>) = with(holder.binding) {
		root.setOnClickListener {
			onItemClick(item!!)
		}
	}

	override fun getItemViewType(position: Int) = R.layout.item_add_location
}