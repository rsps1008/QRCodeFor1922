# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep R8 shrinking enabled while preserving class and member names.
-dontobfuscate

# ML Kit BarcodeScanning accesses parts of its implementation indirectly. The
# bundled consumer rules keep generated proto fields, but R8 full mode can
# still remove implementation classes required by BarcodeScanning.getClient().
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode_bundled.** { *; }

# Google Drive API models are parsed through reflection. Keep their fields and
# accessors so Release R8 shrinking does not cause "key error" at runtime.
-keep class com.google.api.services.drive.** { *; }

# Optional Apache HTTP authentication integrations are not included on Android.
-dontwarn org.apache.http.**
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**

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
