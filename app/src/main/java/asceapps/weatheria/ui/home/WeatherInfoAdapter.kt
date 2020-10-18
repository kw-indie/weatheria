package asceapps.weatheria.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import asceapps.weatheria.data.WeatherInfo
import asceapps.weatheria.databinding.ItemWeatherInfoBinding

class WeatherInfoAdapter(var metric: Boolean = true):
	ListAdapter<WeatherInfo, WeatherInfoAdapter.ViewHolder>(WeatherInfoDiffCallback()) {

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
		holder.bind(getItem(position), metric)
	}

	// todo remove
	override fun getItemId(position: Int): Long {
		return getItem(position).location.id.toLong()
	}

	public override fun getItem(position: Int): WeatherInfo = super.getItem(position)

	class ViewHolder(private val binding: ItemWeatherInfoBinding):
		RecyclerView.ViewHolder(binding.root) {

		fun bind(wi: WeatherInfo, isMetric: Boolean) {
			with(binding) {
				info = wi
				metric = isMetric
				executePendingBindings()
			}
		}
	}

	private class WeatherInfoDiffCallback: DiffUtil.ItemCallback<WeatherInfo>() {

		override fun areItemsTheSame(oldItem: WeatherInfo, newItem: WeatherInfo): Boolean =
			oldItem.location.id == newItem.location.id

		override fun areContentsTheSame(oldItem: WeatherInfo, newItem: WeatherInfo): Boolean =
			oldItem.location.updatedAt == newItem.location.updatedAt
	}
}