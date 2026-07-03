# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/google-sdk/android-sdk/tools/proguard/proguard-android.txt

# Keep application class
-keep class com.example.scanapp.** { *; }

# Kuikly rules
-keep class com.tencent.kuikly.** { *; }
-dontwarn com.tencent.kuikly.**
