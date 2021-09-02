package asceapps.weatheria.util

import android.util.SparseIntArray
import asceapps.weatheria.R

object IconMapper {
	private val map = SparseIntArray(40).apply {
		// todo fix mismatching icons and remove excess
		// https://developer.accuweather.com/weather-icons
		append(1, R.drawable.w_clear_d) // sunny d
		append(2, R.drawable.w_clear_d) // mostly sunny d
		append(3, R.drawable.w_cloudy_p_d) // partly sunny d
		append(4, R.drawable.w_cloudy_p_d) // intermittent clouds d
		append(5, R.drawable.w_cloudy_p_d) // hazy sunshine d
		append(6, R.drawable.w_cloudy_d) // mostly cloudy d
		append(7, R.drawable.w_cloudy_d) // cloudy d/n
		append(8, R.drawable.w_overcast) // overcast d/n
		append(11, R.drawable.w_fog) // fog d/n
		append(12, R.drawable.w_rain_l) // showers d/n
		append(13, R.drawable.w_rain_l_d) // mostly cloudy w/ showers d
		append(14, R.drawable.w_rain_l_d) // partly sunny w/ showers d
		append(15, R.drawable.w_thunder) // t storms d/n
		append(16, R.drawable.w_rain_t_d) // mostly cloudy w/ t storms d
		append(17, R.drawable.w_rain_t_d) // partly sunny w/ t storms d
		append(18, R.drawable.w_rain_l) // rain d/n
		append(19, R.drawable.w_snow_l) // flurries d/n
		append(20, R.drawable.w_snow_l_d) // mostly cloudy w/ flurries d
		append(21, R.drawable.w_sleet_h_d) // partly sunny w/ flurries d
		append(22, R.drawable.w_snow_h) // snow d/n
		append(23, R.drawable.w_snow_h_d) // mostly cloudy w/ snow d
		append(24, R.drawable.w_ice_pellets_l) // ice d/n
		append(25, R.drawable.w_sleet_l) // sleet d/n
		append(26, R.drawable.w_rain_f) // freezing rain d/n
		append(29, R.drawable.w_sleet_l) // rain n snow d/n
		append(30, R.drawable.w_clear_d) // hot d/n
		append(31, R.drawable.w_clear_n) // cold d/n
		append(32, R.drawable.w_mist) // windy d/n
		append(33, R.drawable.w_clear_n) // clear n
		append(34, R.drawable.w_clear_n) // mostly clear n
		append(35, R.drawable.w_cloudy_p_n) // partly cloudy n
		append(36, R.drawable.w_cloudy_p_n) // intermittent clouds n
		append(37, R.drawable.w_cloudy_p_n) // hazy moonshine n
		append(38, R.drawable.w_cloudy_n) // mostly cloudy n
		append(39, R.drawable.w_rain_l_n) // partly cloudy w/ showers n
		append(40, R.drawable.w_rain_h_n) // mostly cloudy w/ showers n
		append(41, R.drawable.w_thunder_n) // partly cloudy w/ t storms n
		append(42, R.drawable.w_rain_t_n) // mostly cloudy w/ t storms n
		append(43, R.drawable.w_snow_l_n) // mostly cloudy w/ flurries n
		append(44, R.drawable.w_snow_h_n) // mostly cloudy w/ snow n
	}

	operator fun get(condition: Int) = map[condition]
}