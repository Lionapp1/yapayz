# ProGuard rules for APK Pro Editor
# Keep DEX parser classes
-keep class org.jf.dexlib2.** { *; }
-keep class org.jf.baksmali.** { *; }

# Keep serialization
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# Keep models
-keep class com.apkpro.editor.data.model.** { *; }
