package asceapps.weatheria.ui.adapter

import asceapps.weatheria.R
import asceapps.weatheria.data.model.WeatherInfo
import asceapps.weatheria.databinding.ItemWeatherInfoBinding

class PagerAdapter: BaseAdapter<WeatherInfo, ItemWeatherInfoBinding>() {

	override fun getItemViewType(position: Int) = R.layout.item_weather_info

	public override fun getItem(position: Int): WeatherInfo {
		return super.getItem(position)
	}
}