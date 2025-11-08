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
-keepclasseswithmembers class * {
    @androidx.room.* <methods>;
}
-keep class * extends androidx.room.RoomDatabase { *; }

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
# Google Mobile Ads           #
###############################
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.**

###############################
# App packages (conservative) #
###############################
# Keep entry Activities and Composables from aggressive stripping
-keep class com.example.wealthtracker.MainActivity { *; }
-keep class com.ss.wealthtracker.MainActivity { *; }
# ViewModels are often created via reflection in Hilt factories
-keep class com.example.wealthtracker.ui.**ViewModel { *; }
-keep class com.ss.wealthtracker.ui.**ViewModel { *; }

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
