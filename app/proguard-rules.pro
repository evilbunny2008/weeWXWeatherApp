# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses SafeWebView with JS, uncomment the following
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
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends androidx.fragment.app.Fragment


########################################
# Common libraries
########################################

# Gson / JSON (keeps model field names)
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Retrofit / OkHttp
-keep class okhttp3.ConnectionSpec { *; }
-keep class okhttp3.Credentials { *; }
-keep class okhttp3.OkHttpClient { *; }
-keep class okhttp3.Request { *; }
-keep class okhttp3.Response { *; }
-dontwarn okhttp3.**

-keep class com.sun.jna.** { *; }
-dontwarn com.sun.jna.**

-keep class sun.net.spi.** { *; }
-dontwarn sun.net.spi.**

-keep class java.net.spi.** { *; }
-dontwarn java.net.spi.**

-keep class javax.naming.** { *; }
-dontwarn javax.naming.**

-keep class lombok.** { *; }
-dontwarn lombok.**

-keep class org.slf4j.** { *; }

-keep class org.xbill.DNS.Cache.** { *; }
-dontwarn org.xbill.DNS.Cache.**

-keep class org.xbill.DNS.Config.** { *; }
-dontwarn org.xbill.DNS.Config.**

-keep class org.xbill.DNS.spi.** { *; }
-dontwarn org.xbill.DNS.spi.**

########################################
# Misc
########################################

# Keep the names of enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep entry point
-keep public class * extends android.app.Application { *; }
-keep public class * extends android.app.Activity { *; }
-keep public class * extends androidx.fragment.app.Fragment { *; }

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