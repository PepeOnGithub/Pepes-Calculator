# Add project specific ProGuard rules here.
-keepattributes *Annotation*

# ML Kit looks up its model identifiers and internals reflectively; R8 must not rename or strip them.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-dontwarn com.google.mlkit.**
