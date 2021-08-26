package asceapps.weatheria.ui.adapter

import asceapps.weatheria.R
import asceapps.weatheria.databinding.ItemAddLocationBinding
import asceapps.weatheria.shared.api.SearchResponse

class AddLocationAdapter(
	private val onItemClick: (SearchResponse) -> Unit
) : BaseAdapter<SearchResponse, ItemAddLocationBinding>() {

	override fun onHolderCreated(holder: BindingHolder<ItemAddLocationBinding>) = with(holder.binding) {
		root.setOnClickListener {
			onItemClick(item!!)
		}
	}

	override fun getItemViewType(position: Int) = R.layout.item_add_location
}