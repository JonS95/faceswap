# ML Kit
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_face.** { *; }

# Keep data/model classes used via reflection by Compose/Coroutines
-dontwarn kotlinx.coroutines.**
