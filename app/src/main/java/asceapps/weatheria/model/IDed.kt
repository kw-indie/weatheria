package asceapps.weatheria.model

interface IDed {

	val id: Int

	override fun hashCode(): Int
}