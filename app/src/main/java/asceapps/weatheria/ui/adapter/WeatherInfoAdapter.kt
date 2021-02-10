package asceapps.weatheria.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.databinding.ItemWeatherInfoBinding
import asceapps.weatheria.model.WeatherInfo

class WeatherInfoAdapter: BaseAdapter<WeatherInfo, WeatherInfoAdapter.ViewHolder>(DiffCallback()) {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(ItemWeatherInfoBinding.inflate(
			LayoutInflater.from(parent.context),
			parent, false
		))
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.bind(getItem(position))
	}

	override fun getItemId(position: Int): Long {
		return list[position].location.id.toLong()
	}

	class ViewHolder(private val binding: ItemWeatherInfoBinding): RecyclerView.ViewHolder(binding.root) {

		fun bind(wi: WeatherInfo) {
			with(binding) {
				info = wi
				executePendingBindings()
			}
		}
	}

	private class DiffCallback: BaseAdapter.DiffCallback<WeatherInfo>() {

		override fun areItemsTheSame(old: WeatherInfo, new: WeatherInfo) =
			old.location.id == new.location.id

		override fun areContentsTheSame(old: WeatherInfo, new: WeatherInfo) =
			old.lastUpdate == new.lastUpdate
	}
}