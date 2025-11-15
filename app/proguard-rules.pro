## Keep Retrofit interfaces and models, OkHttp, and Gson to avoid breaking release networking
-keepattributes Signature
-keepattributes Exceptions
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeInvisibleParameterAnnotations
-keepattributes MethodParameters

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

# Retrofit annotations may reference javax.annotation
-dontwarn javax.annotation.**

# OkHttp/Okio
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-dontwarn okio.**

# Gson
-keep class com.google.gson.** { *; }
-dontwarn sun.misc.**

# Our network DTOs and service (Yahoo Finance API models)
-keep class com.example.wealthtracker.network.** { *; }
-keep interface com.example.wealthtracker.network.StocksService
-keepclassmembers class com.example.wealthtracker.network.** { *; }
## Ensure Retrofit HTTP method annotations on our interfaces are preserved
-keepclassmembers interface com.example.wealthtracker.network.** {
    @retrofit2.http.* <methods>;
}

# Kotlin coroutines metadata
-dontwarn org.jetbrains.annotations.**
-dontwarn kotlinx.coroutines.**
-keep class kotlin.Metadata { *; }
-keep class kotlin.coroutines.** { *; }

###############################
# General debug friendliness  #
###############################
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

###############################
# Kotlin/Coroutines/Compose   #
###############################
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**

###############################
# Dagger/Hilt                 #
###############################
-dontwarn dagger.**
-dontwarn javax.inject.**
-keep class dagger.hilt.** { *; }
-keep class dagger.hilt.internal.** { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class * extends android.app.Application { *; }

###############################
# Room                       #
###############################
-keep class androidx.room.** { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Database class * { *; }
-keepclasseswithmembers class * {
    @androidx.room.* <methods>;
}
-keep class * extends androidx.room.RoomDatabase { *; }
# Keep all Room entities and their fields
-keep class com.example.wealthtracker.data.local.** { *; }
-keep class com.ss.wealthtracker.data.local.** { *; }
-keepclassmembers class com.example.wealthtracker.data.local.** { *; }
-keepclassmembers class com.ss.wealthtracker.data.local.** { *; }

###############################
# Retrofit / OkHttp / Gson    #
###############################
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*,Signature
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
# Keep your API interfaces
-keep interface com.example.wealthtracker.** { *; }
-keep interface com.ss.wealthtracker.** { *; }
# Conservative: keep model classes under common app packages
-keep class com.example.wealthtracker.model.** { *; }
-keep class com.ss.wealthtracker.model.** { *; }

###############################
# Lottie (animations)          #
###############################
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }

###############################
# MPAndroidChart (charts)       #
###############################
-dontwarn com.github.mikephil.charting.**
-keep class com.github.mikephil.charting.** { *; }

###############################
# Firebase Messaging          #
###############################
-keep class * extends com.google.firebase.messaging.FirebaseMessagingService { *; }
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

###############################
# InMobi Ads SDK              #
###############################
-keep class com.inmobi.** { *; }
-dontwarn com.inmobi.**
-keep class com.inmobi.ads.** { *; }
-keep class com.inmobi.media.** { *; }
-keep class com.inmobi.sdk.** { *; }
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep specific InMobi classes referenced in the app
-keep class com.inmobi.ads.InMobiBanner { *; }
-keep class com.inmobi.ads.listeners.BannerAdEventListener { *; }
-keep class com.inmobi.ads.InMobiAdRequestStatus { *; }
-keep class com.inmobi.ads.AdMetaInfo { *; }
-keep class com.inmobi.ads.exceptions.SdkNotInitializedException { *; }
-keep class com.inmobi.sdk.InMobiSdk { *; }
-keep class com.inmobi.sdk.SdkInitializationListener { *; }

###############################
# Google Mobile Ads (Legacy)  #
###############################
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.**

###############################
# Locale and Country Selection  #
###############################
# Keep locale-related classes and methods
-keep class java.util.Locale { *; }
-keep class android.os.LocaleList { *; }
-keep class androidx.core.os.LocaleListCompat { *; }
-keep class androidx.appcompat.app.AppCompatDelegate { *; }
-keepclassmembers class androidx.appcompat.app.AppCompatDelegate {
    public static void setApplicationLocales(androidx.core.os.LocaleListCompat);
}

# Keep country and currency data classes
-keep class com.example.wealthtracker.data.api.CurrencyInfo { *; }
-keep class com.example.wealthtracker.data.api.CountryCurrencyData { *; }
-keep class com.example.wealthtracker.util.CountryInfo { *; }
-keepclassmembers class com.example.wealthtracker.data.api.** { *; }
-keepclassmembers class com.example.wealthtracker.util.CountryCurrency { *; }

# Keep sealed classes for loading states
-keep class com.example.wealthtracker.ui.screens.LoadingState { *; }
-keep class com.example.wealthtracker.ui.screens.LoadingState$* { *; }

# Keep all data classes (they often use reflection)
-keep @kotlin.Metadata class * extends kotlin.coroutines.jvm.internal.BaseContinuationImpl
-keep class **.*$WhenMappings { *; }
-keep class kotlin.Metadata { *; }

# Keep Kotlin data classes and their synthetic methods
-keepclassmembers class * {
    synthetic <methods>;
}
-keepclassmembers class **.*$Companion { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Keep JSON parsing classes
-keep class org.json.** { *; }
-dontwarn org.json.**

###############################
# DataStore Preferences       #
###############################
# Keep DataStore classes and protobuf serialization
-keep class androidx.datastore.** { *; }
-keep class androidx.datastore.preferences.** { *; }
-keep class androidx.datastore.preferences.protobuf.** { *; }
-keep class androidx.datastore.core.** { *; }
-dontwarn androidx.datastore.**

# Keep protobuf classes used by DataStore
-keep class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
    <methods>;
}

# Keep protobuf serialization internals
-keep class androidx.datastore.preferences.protobuf.FieldSet { *; }
-keep class androidx.datastore.preferences.protobuf.FieldSet$* { *; }
-keep class androidx.datastore.preferences.protobuf.MapEntryLite { *; }
-keep class androidx.datastore.preferences.protobuf.MapEntryLite$* { *; }
-keep class androidx.datastore.preferences.protobuf.MapFieldSchemaLite { *; }
-keep class androidx.datastore.preferences.protobuf.MessageSchema { *; }
-keep class androidx.datastore.preferences.protobuf.AbstractMessageLite { *; }

# Keep all protobuf methods that are called via reflection
-keepclassmembers class androidx.datastore.preferences.protobuf.** {
    public <methods>;
    private <methods>;
    protected <methods>;
}

# Prevent obfuscation of protobuf enums and their values
-keepclassmembers enum androidx.datastore.preferences.protobuf.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep PreferencesProto classes
-keep class androidx.datastore.preferences.PreferencesProto { *; }
-keep class androidx.datastore.preferences.PreferencesProto$* { *; }
-keep class androidx.datastore.preferences.PreferencesMapCompat { *; }
-keep class androidx.datastore.preferences.PreferencesMapCompat$* { *; }

# Keep serializers
-keep class androidx.datastore.preferences.core.PreferencesSerializer { *; }
-keepclassmembers class androidx.datastore.preferences.core.PreferencesSerializer {
    public <methods>;
}

# Keep exception classes for DataStore error handling
-keep class androidx.datastore.core.CorruptionException { *; }
-keep class java.io.IOException { *; }

# Keep coroutines exception handling
-keep class kotlinx.coroutines.flow.** { *; }
-keepclassmembers class kotlinx.coroutines.flow.** {
    public <methods>;
}

# Prevent aggressive optimization of protobuf serialization logic
-keepclassmembers class androidx.datastore.preferences.protobuf.FieldSet {
    private static *** computeElementSizeNoTag(...);
    private static *** computeElementSize(...);
    *** computeElementSizeNoTag(...);
    *** computeElementSize(...);
}
-keepclassmembers class androidx.datastore.preferences.protobuf.MapEntryLite {
    *** computeSerializedSize(...);
    *** computeMessageSize(...);
}
-keepclassmembers class androidx.datastore.preferences.protobuf.MapFieldSchemaLite {
    *** getSerializedSizeLite(...);
    *** getSerializedSize(...);
}

# Keep all switch statements and enum handling in protobuf
-keepclassmembers class androidx.datastore.preferences.protobuf.** {
    *** ordinal();
    *** name();
    *** values();
    *** valueOf(...);
}

# Keep WireFormat and related classes that handle field types
-keep class androidx.datastore.preferences.protobuf.WireFormat { *; }
-keep class androidx.datastore.preferences.protobuf.WireFormat$* { *; }

# Keep DataStore OkIO storage classes
-keep class androidx.datastore.core.okio.** { *; }
-keepclassmembers class androidx.datastore.core.okio.** {
    <methods>;
}

###############################
# App packages (conservative) #
###############################
# Keep entry Activities and Composables from aggressive stripping
-keep class com.example.wealthtracker.MainActivity { *; }
-keep class com.ss.wealthtracker.MainActivity { *; }
# ViewModels are often created via reflection in Hilt factories
-keep class com.example.wealthtracker.ui.**ViewModel { *; }
-keep class com.ss.wealthtracker.ui.**ViewModel { *; }
# Keep repositories and data sources
-keep class com.example.wealthtracker.data.repository.** { *; }
-keep class com.ss.wealthtracker.data.repository.** { *; }
# Keep data classes used in the app
-keep class com.example.wealthtracker.data.** { *; }
-keep class com.ss.wealthtracker.data.** { *; }

###############################
# Jetpack Compose            #
###############################
-keep class androidx.compose.** { *; }
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
# Keep Compose state holders
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

###############################
# SQLCipher (native JNI)       #
###############################
# Prevent obfuscation/stripping of classes and native methods used by SQLCipher.
-dontwarn net.sqlcipher.**
-keep class net.sqlcipher.** { *; }
-keepclassmembers class net.sqlcipher.** { *; }
# Preserve and KEEP all native methods so RegisterNatives can find them
-keepclasseswithmembers class * { native <methods>; }
-keepclasseswithmembers class net.sqlcipher.** { native <methods>; }
