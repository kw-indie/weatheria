package asceapps.weatheria.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.databinding.ItemWeatherInfoBinding
import asceapps.weatheria.model.WeatherInfo

class WeatherInfoAdapter : BaseAdapter<WeatherInfo, WeatherInfoAdapter.ViewHolder>() {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(
			ItemWeatherInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.bind(getItem(position))
	}

	class ViewHolder(
		private val binding: ItemWeatherInfoBinding
	) : RecyclerView.ViewHolder(binding.root) {

		fun bind(wi: WeatherInfo) {
			with(binding) {
				info = wi
				executePendingBindings()
			}
		}
	}
}