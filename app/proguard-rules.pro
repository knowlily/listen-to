# Retrofit/Room/Hilt are not used, but safe defaults for WebView + Kotlin

# Keep WebView JavaScript interface (if any added later)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin serialization / metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# Keep Room entities
-keep class com.knowlily.browser.model.** { *; }

# Keep plugin interface
-keep interface com.knowlily.browser.plugin.BrowserPlugin { *; }
-keep class com.knowlily.browser.plugin.UserPluginConfig { *; }
