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

-keep,allowoptimization class com.odiousapps.weewxweather.** { *; }

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

# Excess JSoup features not used but warned about
-dontwarn com.google.re2j.**

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
-keepclassmembers enum *
{
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep entry point
-keep public class * extends android.app.Application { *; }
-keep public class * extends android.app.Activity { *; }
-keep public class * extends androidx.fragment.app.Fragment { *; }

########################################
# Logging
########################################

# Leave important logging lines in...
#    public static int i(...);
#    public static int w(...);
#    public static int e(...);

# Remove all Log calls (makes app smaller)
-assumenosideeffects class android.util.Log
{
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

########################################
# Gson/Json classes
########################################

# Keep generic signatures; needed for correct type resolution
-keepattributes Signature

# Keep Gson annotations
# Note: Cannot perform finer selection here to only cover Gson annotations, see also https://stackoverflow.com/q/47515093
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

### The following rules are needed for R8 in "full mode" which only adheres to `-keepattribtues` if
### the corresponding class or field is matches by a `-keep` rule as well, see
### https://r8.googlesource.com/r8/+/refs/heads/main/compatibility-faq.md#r8-full-mode

# Keep class TypeToken (respectively its generic signature) if present
-if class com.google.gson.reflect.TypeToken
-keep,allowobfuscation class com.google.gson.reflect.TypeToken

# Keep any (anonymous) classes extending TypeToken
-keep,allowobfuscation class * extends com.google.gson.reflect.TypeToken

# Keep classes with @JsonAdapter annotation
-keep,allowobfuscation,allowoptimization @com.google.gson.annotations.JsonAdapter class *

# Keep fields with any other Gson annotation
# Also allow obfuscation, assuming that users will additionally use @SerializedName or
# other means to preserve the field names
-keepclassmembers,allowobfuscation class *
{
  @com.google.gson.annotations.Expose <fields>;
  @com.google.gson.annotations.JsonAdapter <fields>;
  @com.google.gson.annotations.Since <fields>;
  @com.google.gson.annotations.Until <fields>;
}

# Keep no-args constructor of classes which can be used with @JsonAdapter
# By default their no-args constructor is invoked to create an adapter instance
-keepclassmembers class * extends com.google.gson.TypeAdapter { <init>(); }
-keepclassmembers class * implements com.google.gson.TypeAdapterFactory { <init>(); }
-keepclassmembers class * implements com.google.gson.JsonSerializer { <init>(); }
-keepclassmembers class * implements com.google.gson.JsonDeserializer { <init>(); }

# Keep fields annotated with @SerializedName for classes which are referenced.
# If classes with fields annotated with @SerializedName have a no-args
# constructor keep that as well. Based on
# https://issuetracker.google.com/issues/150189783#comment11.
# See also https://github.com/google/gson/pull/2420#discussion_r1241813541
# for a more detailed explanation.
-if class *
-keepclasseswithmembers,allowobfuscation class <1> { @com.google.gson.annotations.SerializedName <fields>; }

-if class * { @com.google.gson.annotations.SerializedName <fields>; }
-keepclassmembers,allowobfuscation,allowoptimization class <1> { <init>(); }