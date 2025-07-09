# Keep rules for Jetpack Compose
-keep class androidx.compose.** { *; }
-keep class androidx.activity.compose.** { *; }
-keep class androidx.lifecycle.** { *; }

# Keep rules for Firebase Messaging
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep rules for Clix SDK (패키지명에 맞게 수정)
-keep class so.clix.** { *; }
-dontwarn so.clix.** 