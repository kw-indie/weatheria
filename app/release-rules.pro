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

# check for hints https://medium.com/androiddevelopers/practical-proguard-rules-examples-5640a3907dc9