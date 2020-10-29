package asceapps.weatheria.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.databinding.ItemWeatherInfoBinding
import asceapps.weatheria.model.WeatherInfo

class WeatherInfoAdapter: ListAdapter<WeatherInfo, WeatherInfoAdapter.ViewHolder>(DiffCallback()) {

	init {
		stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(ItemWeatherInfoBinding.inflate(
			LayoutInflater.from(parent.context),
			parent, false
		))
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.bind(getItem(position))
	}

	public override fun getItem(position: Int): WeatherInfo = super.getItem(position)

	class ViewHolder(private val binding: ItemWeatherInfoBinding):
		RecyclerView.ViewHolder(binding.root) {

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
			oldItem.updateTime == newItem.updateTime
	}
}