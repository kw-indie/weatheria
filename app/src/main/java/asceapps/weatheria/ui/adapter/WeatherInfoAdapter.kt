package asceapps.weatheria.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.databinding.ItemWeatherInfoBinding
import asceapps.weatheria.model.WeatherInfo

class WeatherInfoAdapter: BaseListAdapter<WeatherInfo, WeatherInfoAdapter.ViewHolder>(DiffCallback()) {

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
		return currentList[position].location.id.toLong()
	}

	class ViewHolder(private val binding: ItemWeatherInfoBinding): RecyclerView.ViewHolder(binding.root) {

		fun bind(wi: WeatherInfo) {
			with(binding) {
				info = wi
				executePendingBindings()
			}
		}
	}

	private class DiffCallback: DiffUtil.ItemCallback<WeatherInfo>() {

		override fun areItemsTheSame(oldItem: WeatherInfo, newItem: WeatherInfo) =
			oldItem.location.id == newItem.location.id

		override fun areContentsTheSame(oldItem: WeatherInfo, newItem: WeatherInfo) =
			oldItem.lastUpdate == newItem.lastUpdate
	}
}