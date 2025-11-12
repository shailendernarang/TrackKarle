## OPTIMIZED ProGuard Rules - Only keep what's necessary for reflection/serialization

###############################
# General Attributes          #
###############################
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
-keepattributes SourceFile,LineNumberTable
-keepattributes InnerClasses,EnclosingMethod

###############################
# Kotlin                      #
###############################
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }

###############################
# Dagger/Hilt (DI)            #
###############################
-dontwarn dagger.**
-dontwarn javax.inject.**
# Only keep generated components, not entire Hilt library
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class * extends android.app.Application { *; }

###############################
# Room Database               #
###############################
# Only keep annotated classes, not entire library
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Dao class * { *; }
# Keep our specific entity
-keep class com.example.wealthtracker.data.local.InvestmentEntity { *; }
-keep class com.ss.wealthtracker.data.local.InvestmentEntity { *; }
-keep class com.example.wealthtracker.data.local.InvestmentDao { *; }
-keep class com.ss.wealthtracker.data.local.InvestmentDao { *; }
-keep class com.example.wealthtracker.data.local.AppDatabase { *; }
-keep class com.ss.wealthtracker.data.local.AppDatabase { *; }

###############################
# Retrofit/Gson (Networking)  #
###############################
# Don't keep entire libraries, only what Retrofit needs for reflection
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
# Keep our API models and service interfaces
-keep class com.example.wealthtracker.network.** { *; }
-keep class com.ss.wealthtracker.network.** { *; }
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}

###############################
# Jetpack Compose             #
###############################
# Only keep Composable functions, not entire framework
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

###############################
# ViewModels (Hilt injection) #
###############################
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class com.example.wealthtracker.ui.**ViewModel { *; }
-keep class com.ss.wealthtracker.ui.**ViewModel { *; }

###############################
# MainActivity                #
###############################
-keep class com.example.wealthtracker.MainActivity { *; }
-keep class com.ss.wealthtracker.MainActivity { *; }

###############################
# SQLCipher (JNI)             #
###############################
-dontwarn net.sqlcipher.**
-keep class net.sqlcipher.** { *; }
-keepclasseswithmembers class * { native <methods>; }

###############################
# Firebase & AdMob            #
###############################
-keep class * extends com.google.firebase.messaging.FirebaseMessagingService { *; }
-keep class com.google.android.gms.ads.AdView { *; }

###############################
# Third-party libs (minimal)  #
###############################
# Lottie - only keep what's used via reflection
-keep class com.airbnb.lottie.** { *; }
# MPAndroidChart
-dontwarn com.github.mikephil.charting.**
-keep class com.github.mikephil.charting.** { *; }

###############################
# Warnings suppression        #
###############################
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**
-dontwarn kotlinx.coroutines.**
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
