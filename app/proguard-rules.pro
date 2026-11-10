## R8 rules.
## Keep this file small: Compose, Firebase, OkHttp, Retrofit, Gson, Room, Hilt, DataStore,
## Lottie and Google Play services all ship consumer rules in their AARs. Blanket
## "-keep class lib.** { *; }" rules disable shrinking/optimization/obfuscation for the
## whole library and drag Play Console's DEX optimization scores down.

## Facebook Infer annotations used by Meta Ads SDK — not shipped in the AAR
-dontwarn com.facebook.infer.annotation.**
-dontwarn com.facebook.ads.**

## WorkManager (transitive via Appodeal adapters) — R8 on the AGP9 toolchain has been
## observed stripping WorkDatabase_Impl's generated constructor, breaking WorkManager init.
-keep class androidx.work.impl.WorkDatabase_Impl { <init>(...); }
-dontwarn androidx.work.**

## Strip verbose logging in release builds (Log.e kept for genuine errors)
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
    public static boolean isLoggable(java.lang.String, int);
}

## Attributes needed by Retrofit/Gson reflection; line numbers kept for crash deobfuscation
-keepattributes Signature,Exceptions,*Annotation*,InnerClasses,EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

## Put everything obfuscated into the root package (smaller dex, higher obfuscation score)
-repackageclasses ''
-allowaccessmodification

###############################
# Gson / Retrofit DTOs        #
###############################
# Gson maps JSON by field name via reflection, so DTO classes must keep their fields.
-keep class com.example.wealthtracker.network.** { *; }
-keep class com.example.wealthtracker.data.api.** { *; }
-keep class com.example.wealthtracker.data.remote.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn javax.annotation.**
-dontwarn sun.misc.**

###############################
# SQLCipher (native JNI)      #
###############################
# Native code registers/looks up these classes by name; AppModule also loads
# SupportFactory via Class.forName.
-dontwarn net.sqlcipher.**
-keep class net.sqlcipher.** { *; }

###############################
# InMobi Ads SDK              #
###############################
-keep class com.inmobi.** { *; }
-dontwarn com.inmobi.**
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
