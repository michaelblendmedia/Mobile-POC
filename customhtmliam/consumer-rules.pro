# Custom HTML IAM — consumer ProGuard/R8 rules.
# Applied automatically to any app that depends on this library (via consumerProguardFiles),
# so integrators do NOT need to copy these rules into their own proguard-rules.pro.
#
# The WebView JS bridge is invoked only from JavaScript at runtime, so R8 cannot see the callers
# and would otherwise strip the class and its @JavascriptInterface methods.
-keep class com.sfmc.customhtmliam.CustomHtmlBridge { *; }
-keepclassmembers class com.sfmc.customhtmliam.CustomHtmlBridge {
    @android.webkit.JavascriptInterface <methods>;
}
