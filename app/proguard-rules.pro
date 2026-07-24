# proguard-rules.pro
# MediaCollectorApp R8 / ProGuard 混淆规则

# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.mediacollector.app.**$$serializer { *; }
-keepclassmembers class com.mediacollector.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.mediacollector.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Retrofit
-keep,allowobfuscation interface retrofit2.Call
-keep,allowobfuscation interface retrofit2.Response
-keep,allowobfuscation class retrofit2.http.** { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Coil
-keep class coil3.** { *; }
-dontwarn coil3.PlatformContext

# Keep Media3 ExoPlayer
-keep class androidx.media3.** { *; }

# Keep MQTT
-keep class org.eclipse.paho.** { *; }

# Keep Gson / serialization
-keep class kotlinx.serialization.** { *; }

# Keep Cast SDK
-keep class com.google.android.gms.cast.** { *; }
