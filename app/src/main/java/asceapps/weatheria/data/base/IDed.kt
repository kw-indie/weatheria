package asceapps.weatheria.data.base

interface IDed {

	val id: Int

	override fun hashCode(): Int
}