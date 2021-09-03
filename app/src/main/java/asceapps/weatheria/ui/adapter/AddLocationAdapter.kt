package asceapps.weatheria.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import asceapps.weatheria.R
import asceapps.weatheria.databinding.ItemAddLocationBinding
import asceapps.weatheria.shared.data.model.Location

class AddLocationAdapter(
	private val onItemClick: (Location) -> Unit
): BaseAdapter<Location, ItemAddLocationBinding>() {

	override fun createBinding(inflater: LayoutInflater, parent: ViewGroup) =
		ItemAddLocationBinding.inflate(inflater, parent, false)

	override fun onHolderCreated(holder: BindingHolder<Location, ItemAddLocationBinding>) {
		holder.binding.root.setOnClickListener {
			onItemClick(holder.item!!)
		}
	}

	override fun onHolderBound(holder: BindingHolder<Location, ItemAddLocationBinding>, item: Location) {
		holder.binding.apply {
			tvName.text = item.name
			tvCountry.text = root.resources.getString(R.string.f_2s, item.country, item.countryFlag)
		}
	}
}