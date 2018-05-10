# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/kplese/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-dontwarn com.squareup.okhttp3.internal.**
-dontwarn com.squareup.okhttp3.**
-dontwarn javax.annotation.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn android.support.v4.**
-dontwarn com.google.android.gms.internal.**
-dontwarn com.fasterxml.jackson.core.**
-dontwarn java.lang.management.**
-dontwarn com.google.android.gms.**
-keep class * {
    public private *;
}
-keep public class com.google.android.gms.*

# Class names are needed in reflection
-keepnames class com.amazonaws.**
-keepnames class com.amazon.**
# Request handlers defined in request.handlers
-keep class com.amazonaws.services.**.*Handler
# The following are referenced but aren't required to run
-dontwarn com.fasterxml.jackson.**
-dontwarn org.apache.commons.logging.**
# Android 6.0 release removes support for the Apache HTTP client
-dontwarn org.apache.http.**
# The SDK has several references of Apache HTTP client
-dontwarn com.amazonaws.http.**
-dontwarn com.amazonaws.metrics.**
