# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for debugging stack traces.
#-dontobfuscate
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to hide the original source file name.
#-renamesourcefileattribute SourceFile
# puts all classes in root package to save space by omitting 'package x.y.z'
-repackageclasses

# Fix maps v3 crash (taken from official docs):
-keep,allowoptimization class com.google.android.libraries.maps.** {*;}

# todo test without this rule since we only need annotations and they should be preserved with other android rules
# Replaces all the @Keep's in response classes
-keepclassmembers,allowoptimization,allowobfuscation class asceapps.weatheria.data.api.**Response** {
#  @com.google.gson.annotations.SerializedName <fields>;
#  @asceapps.weatheria.data.api.Flatten <fields>;
  # the above 2 lines are effectively like the one below
  <fields>;
}

# Ignore annotation used for build tooling. (specified in okio rules)
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement