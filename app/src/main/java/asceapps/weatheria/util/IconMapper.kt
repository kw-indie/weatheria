package asceapps.weatheria.util

import asceapps.weatheria.R

object IconMapper {

	// todo fix mismatching icons and remove excess
	// https://developer.accuweather.com/weather-icons
	private val icons = intArrayOf(
		R.drawable.w_clear_d,  // sunny d
		R.drawable.w_clear_d,  // mostly sunny d
		R.drawable.w_cloudy_p_d,  // partly sunny d
		R.drawable.w_cloudy_p_d,  // intermittent clouds d
		R.drawable.w_cloudy_p_d,  // hazy sunshine d
		R.drawable.w_cloudy_d,  // mostly cloudy d
		R.drawable.w_cloudy_d,  // cloudy d/n
		R.drawable.w_overcast,  // overcast d/n
		R.drawable.w_fog,  // fog d/n
		R.drawable.w_rain_l,  // showers d/n
		R.drawable.w_rain_l_d,  // mostly cloudy w/ showers d
		R.drawable.w_rain_l_d,  // partly sunny w/ showers d
		R.drawable.w_thunder,  // t storms d/n
		R.drawable.w_rain_t_d,  // mostly cloudy w/ t storms d
		R.drawable.w_rain_t_d,  // partly sunny w/ t storms d
		R.drawable.w_rain_l,  // rain d/n
		R.drawable.w_snow_l,  // flurries d/n
		R.drawable.w_snow_l_d,  // mostly cloudy w/ flurries d
		R.drawable.w_sleet_h_d,  // partly sunny w/ flurries d
		R.drawable.w_snow_h,  // snow d/n
		R.drawable.w_snow_h_d,  // mostly cloudy w/ snow d
		R.drawable.w_ice_pellets_l,  // ice d/n
		R.drawable.w_sleet_l,  // sleet d/n
		R.drawable.w_rain_f,  // freezing rain d/n
		R.drawable.w_sleet_l,  // rain n snow d/n
		R.drawable.w_clear_d,  // hot d/n
		R.drawable.w_clear_n,  // cold d/n
		R.drawable.w_mist,  // windy d/n
		R.drawable.w_clear_n,  // clear n
		R.drawable.w_clear_n,  // mostly clear n
		R.drawable.w_cloudy_p_n,  // partly cloudy n
		R.drawable.w_cloudy_p_n,  // intermittent clouds n
		R.drawable.w_cloudy_p_n,  // hazy moonshine n
		R.drawable.w_cloudy_n,  // mostly cloudy n
		R.drawable.w_rain_l_n,  // partly cloudy w/ showers n
		R.drawable.w_rain_h_n,  // mostly cloudy w/ showers n
		R.drawable.w_thunder_n,  // partly cloudy w/ t storms n
		R.drawable.w_rain_t_n,  // mostly cloudy w/ t storms n
		R.drawable.w_snow_l_n,  // mostly cloudy w/ flurries n
		R.drawable.w_snow_h_n,  // mostly cloudy w/ snow n
	)

	operator fun get(condition: Int) = icons[condition]
}