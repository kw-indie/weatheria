package asceapps.weatheria.shared.data.base

interface IconMapper {
	/**
	 * return a drawable resId for this weather condition
	 */
	operator fun get(condition: Int): Int
}