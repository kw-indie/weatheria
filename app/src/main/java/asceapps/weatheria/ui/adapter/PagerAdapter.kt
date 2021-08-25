package asceapps.weatheria.ui.adapter

import asceapps.weatheria.R
import asceapps.weatheria.databinding.ItemWeatherInfoBinding
import asceapps.weatheria.shared.data.model.WeatherInfo
import asceapps.weatheria.ui.view.WeatherChart

class PagerAdapter: BaseAdapter<WeatherInfo, ItemWeatherInfoBinding>() {

	override fun getItemViewType(position: Int) = R.layout.item_weather_info

	public override fun getItem(position: Int): WeatherInfo {
		return super.getItem(position)
	}

	override fun onBindHolder(holder: BindingHolder<ItemWeatherInfoBinding>, item: WeatherInfo) {
		holder.binding.apply {
			hourlyChart.setInfo(item, WeatherChart.HOURLY)
			dailyChart.setInfo(item, WeatherChart.DAILY)
		}
	}
}