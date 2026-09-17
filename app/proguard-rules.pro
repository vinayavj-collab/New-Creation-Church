# ProGuard & R8 Optimization & Obfuscation Rules

# Preserve JavaScript Interface methods for WebView
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep WebViews and their clients
-keepclassmembers class * extends android.webkit.WebViewClient {
    public *;
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public *;
}
-keepclassmembers class * extends android.webkit.WebSettings {
    public *;
}

# Keep Data models for Blogger, YouTube, Bible, Notes, Lyrics, Settings
-keep class com.example.data.model.** { *; }
-keep class com.example.data.bible.model.** { *; }
-keep class com.example.data.bible.entity.** { *; }
-keep class com.example.data.bible.local.** { *; }
-keep class com.example.data.local.** { *; }
-keep class com.example.data.database.** { *; }

# Retrofit & OkHttp & Moshi rules
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes InnerClasses, EnclosingMethod

# Moshi rules
-dontwarn com.squareup.moshi.**
-keep class com.squareup.moshi.** { *; }
-keep class * extends com.squareup.moshi.JsonAdapter { *; }

# Room Database
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**

# Preserve source file & line numbers for debugging
-keepattributes SourceFile,LineNumberTable

