# ProGuard 规则 - MediaCollectorApp

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class com.mediacollector.app.data.remote.dto.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Coil
-keep class coil3.** { *; }

# MQTT
-keep class org.eclipse.paho.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# GMS Cast
-dontwarn com.google.android.gms.**
