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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

########################################
# General Android rules
########################################

# Keep all Activities, Fragments, Services, BroadcastReceivers, etc.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.Application

########################################
# Common libraries
########################################

# Gson / JSON (keeps model field names)
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Retrofit / OkHttp
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Room / SQLite
-keep class androidx.room.** { *; }
-keep @androidx.room.Dao class * { *; }

########################################
# Misc
########################################

# Keep everything in your app’s main package (optional)
# Replace com.example.myapp with your package name
-keep class com.example.myapp.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep the names of enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep entry point
-keep public class * extends android.app.Application { *; }

########################################
# Logging (optional)
########################################

# Remove all Log calls (makes app smaller)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}