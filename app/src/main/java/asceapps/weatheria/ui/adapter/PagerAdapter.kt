package asceapps.weatheria.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import asceapps.weatheria.R
import asceapps.weatheria.databinding.ItemWeatherInfoBinding
import asceapps.weatheria.ext.appendAccuracy
import asceapps.weatheria.ext.setDirectionDrawable
import asceapps.weatheria.shared.data.model.WeatherInfo
import asceapps.weatheria.ui.view.WeatherChart
import asceapps.weatheria.util.Formatter
import asceapps.weatheria.util.IconMapper

class PagerAdapter: BaseAdapter<WeatherInfo, ItemWeatherInfoBinding>() {

	public override fun getItem(position: Int): WeatherInfo {
		return super.getItem(position)
	}

	override fun createBinding(inflater: LayoutInflater, parent: ViewGroup) =
		ItemWeatherInfoBinding.inflate(inflater, parent, false)

	override fun onHolderBound(holder: BindingHolder<WeatherInfo, ItemWeatherInfoBinding>, item: WeatherInfo) {
		holder.binding.apply {
			tvLocation.text = item.location.name
			tvTime.text = Formatter.zonedNow(item.location.zoneId)
			val res = root.resources
			tvLastUpdate.text = res.getString(R.string.f_last_update, Formatter.relativeTime(item.lastUpdate))
			tvLastUpdate.appendAccuracy(item.accuracy)
			ivIcon.setImageResource(IconMapper[item.current.iconIndex])
			tvTemp.text = Formatter.temp(item.current.temp)
			tvFeelsLike.text = res.getString(R.string.f_feels_like, Formatter.temp(item.current.feelsLike))
			tvWeather.text = res.getStringArray(R.array.weather_conditions)[item.current.iconIndex]
			tvWind.text = res.getString(
				R.string.f_2s,
				Formatter.distance(item.current.windSpeed),
				res.getStringArray(R.array.units_speed)[Formatter.unitSystem]
			)
			tvWind.setDirectionDrawable(item.current.windDir)
			tvHumidity.text = Formatter.percent(item.current.humidity)
			tvPressure.text = res.getString(
				R.string.f_2s,
				Formatter.pressure(item.current.pressure),
				res.getStringArray(R.array.units_pressure)[Formatter.unitSystem]
			)
			tvClouds.text = Formatter.percent(item.current.clouds)
			tvDewPoint.text = Formatter.temp(item.current.dewPoint)
			tvVisibility.text = res.getString(
				R.string.f_2s,
				Formatter.distance(item.current.visibility),
				res.getStringArray(R.array.units_distance)[Formatter.unitSystem]
			)
			tvUv.text = res.getString(
				R.string.f_2s_p,
				Formatter.number(item.current.uv),
				res.getStringArray(R.array.uv_levels)[Formatter.uvLevel(item.current.uv)]
			)
			val today = item.today
			tvMax.text = Formatter.temp(today.max)
			tvMin.text = Formatter.temp(today.min)
			tvSunrise.text = Formatter.zonedTime(today.sunrise, item.location.zoneId)
			tvSunset.text = Formatter.zonedTime(today.sunset, item.location.zoneId)
			tvPop.text = Formatter.percent(today.pop)
			tvMoonrise.text = Formatter.zonedTime(today.moonrise, item.location.zoneId)
			tvMoonset.text = Formatter.zonedTime(today.moonset, item.location.zoneId)
			hourlyChart.setInfo(item, WeatherChart.HOURLY)
			dailyChart.setInfo(item, WeatherChart.DAILY)
		}
	}
}