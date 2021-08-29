package asceapps.weatheria.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import asceapps.weatheria.R
import asceapps.weatheria.databinding.ItemWeatherInfoBinding
import asceapps.weatheria.shared.data.model.*
import asceapps.weatheria.ui.view.WeatherChart
import asceapps.weatheria.util.appendAccuracy
import asceapps.weatheria.util.setDirectionDrawable

class PagerAdapter: BaseAdapter<WeatherInfo, ItemWeatherInfoBinding>() {

	public override fun getItem(position: Int): WeatherInfo {
		return super.getItem(position)
	}

	override fun createBinding(inflater: LayoutInflater, parent: ViewGroup) =
		ItemWeatherInfoBinding.inflate(inflater, parent, false)

	override fun onHolderBound(holder: BindingHolder<WeatherInfo, ItemWeatherInfoBinding>, item: WeatherInfo) {
		holder.binding.apply {
			tvLocation.text = item.location.name
			tvTime.text = zonedNow(item.location.zoneId)
			val res = root.resources
			tvLastUpdate.text = res.getText(R.string.f_last_update, relativeTime(item.lastUpdate))
			tvLastUpdate.appendAccuracy(item.accuracy)
			ivIcon.setImageResource(item.current.iconResId)
			tvTemp.text = item.current.temp.toString()
			tvFeelsLike.text = res.getString(R.string.f_feels_like, item.current.feelsLike.toString())
			tvWeather.text = res.getStringArray(R.array.weather_conditions)[item.current.conditionIndex]
			tvWind.text = res.getString(
				R.string.f_2s,
				item.current.windSpeed.toString(),
				res.getStringArray(R.array.units_speed)[unitSystem]
			)
			tvWind.setDirectionDrawable(item.current.windDir)
			tvHumidity.text = item.current.humidity.toString()
			tvPressure.text = res.getString(
				R.string.f_2s,
				item.current.pressure.toString(),
				res.getStringArray(R.array.units_pressure)[unitSystem]
			)
			tvClouds.text = item.current.clouds.toString()
			tvDewPoint.text = item.current.dewPoint.toString()
			tvVisibility.text = res.getString(
				R.string.f_2s,
				item.current.visibility.toString(),
				res.getStringArray(R.array.units_distance)[unitSystem]
			)
			tvUv.text = res.getString(
				R.string.f_2s_p,
				item.current.uv.toString(),
				res.getStringArray(R.array.uv_levels)[item.current.uv.level]
			)
			val today = item.today // today(item.daily) // todo when this is null?
			tvMax.text = today.max.toString()
			tvMin.text = today.min.toString()
			tvSunrise.text = zonedTime(today.sunrise, item.location.zoneId)
			tvSunset.text = zonedTime(today.sunset, item.location.zoneId)
			tvPop.text = today.pop.toString()
			tvMoonrise.text = zonedTime(today.moonrise, item.location.zoneId)
			tvMoonset.text = zonedTime(today.moonset, item.location.zoneId)
			hourlyChart.setInfo(item, WeatherChart.HOURLY)
			dailyChart.setInfo(item, WeatherChart.DAILY)
		}
	}
}