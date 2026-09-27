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

# Required for readable Crashlytics stack traces: without these, a deobfuscated release
# trace still has no line numbers, which makes most crash reports undiagnosable.
# -renamesourcefileattribute hides the real file name while keeping the attribute R8 needs
# to emit line numbers at all. Declared explicitly rather than relying on whatever the
# bundled proguard-android-optimize.txt happens to contain; -keepattributes is additive.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
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
