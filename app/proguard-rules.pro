# ProGuard / R8 rules for Fast Note (Android Auto Note)

# ── 1. Retain debugging attributes for Crash logs & Stack traces ──
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod

# ── 2. Entire Project Codebase & Sub-packages ──
# Bảo vệ 100% tất cả các class, models, services, UI, utils, widgets của Fast Note
-keep class com.tatl.fastnote.** { *; }
-keepclassmembers class com.tatl.fastnote.** { *; }
-keep class * extends android.app.Activity
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider
-keep class * extends android.appwidget.AppWidgetProvider

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── 3. Room Database ──
-keep class androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.migration.Migration { *; }

# ── 4. Kotlin Coroutines ──
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ── 5. OkHttp & Okio ──
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# ── 6. Google Play Billing ──
-keep class com.android.billingclient.api.** { *; }
-dontwarn com.android.billingclient.api.**

# ── 7. Google Play Services & Google Sign-In ──
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.android.gms.common.api.** { *; }
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-dontwarn com.google.android.gms.**

# ── 8. Firebase Auth & Firestore ──
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ── 9. Zip4j (AES encryption for Send PC) ──
-keep class net.lingala.zip4j.** { *; }
-dontwarn net.lingala.zip4j.**

# ── 10. Glance App Widgets ──
-keep class androidx.glance.** { *; }
-keep class com.tatl.fastnote.widget.** { *; }

# ── 11. Per-App Language (AppCompatDelegate.setApplicationLocales) ──
-keep class androidx.appcompat.app.AppCompatDelegate { *; }
-keep class androidx.core.os.LocaleListCompat { *; }