# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

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
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Keep WeatherForecast classes to prevent class casting issues
-keep class bose.ankush.weatherify.domain.model.WeatherForecast { *; }
-keep class bose.ankush.weatherify.domain.model.WeatherForecast$* { *; }
-keep class bose.ankush.network.model.WeatherForecast { *; }
-keep class bose.ankush.network.model.WeatherForecast$* { *; }
-keep class bose.ankush.weatherify.domain.model.WeatherCondition { *; }

# Keep storage model classes
-keep class bose.ankush.storage.room.WeatherEntity { *; }
-keep class bose.ankush.storage.room.WeatherEntity$* { *; }
-keep class bose.ankush.storage.room.Weather { *; }
-keep class bose.ankush.network.model.WeatherCondition { *; }
