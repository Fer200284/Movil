# Reglas de conservación para Retrofit, Gson y Room DB al generar APKs (Release / Signed APK)

-keep class com.sistema.paciencare.data.remote.dto.** { *; }
-keep class com.sistema.paciencare.data.local.entities.** { *; }
-keep class com.sistema.paciencare.model.** { *; }

-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit & OkHttp
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn com.google.gson.**
