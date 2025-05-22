# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontwarn android.app.ActivityThread
-dontwarn android.app.ContextImpl
-dontwarn android.app.IActivityManager
-dontwarn android.content.IIntentReceiver$Stub
-dontwarn android.content.IIntentReceiver
-dontwarn android.content.IIntentSender
-dontwarn android.content.pm.IPackageManager

# 会反射 / jni 中获取，保留类及其全部属性+方法声明及其名称
-keep class com.termux.x11.MainActivity {
    <fields>;
    <methods>;
}
-keep class com.termux.x11.CmdEntryPoint {
    <fields>;
    <methods>;
}
-keep class com.termux.x11.LoriePreferences {
    <fields>;
    <methods>;
}
-keep class com.termux.x11.Prefs {
    <fields>;
    <methods>;
}
-keep class org.github.ewt45.winemulator.MainEmuActivity {
    <fields>;
    <methods>;
}
-keep class org.github.ewt45.winemulator.Consts {
    <fields>;
    <methods>;
}


