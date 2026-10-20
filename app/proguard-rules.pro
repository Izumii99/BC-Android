# Keep JavascriptInterface methods accessible
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep WebKit and OkHttp
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep custom WebView subclasses used in layouts
-keep class com.izumii.bcandroid.KeepAliveWebView { *; }
