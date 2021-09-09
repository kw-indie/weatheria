-keep,allowoptimization class asceapps.weatheria.gmap.MapsFragment {
	<init>(...);
}
-keepclassmembers class asceapps.weatheria.gmap.R$* {
	<fields>; # keeps the fields in R while keep.xml keeps the res they point to
}

# Fix maps v3 crash (taken from official docs):
-keep,allowoptimization class com.google.android.libraries.maps.** {*;}