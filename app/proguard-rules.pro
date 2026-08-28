# ProGuard / R8 rules for Fast Note (Android Auto Note)

# ── 1. Retain debugging attributes for Crash logs & Stack traces ──
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod

# ── 2. Data Models, Enums & Serialization ──
-keep class com.tatl.fastnote.data.** { *; }
-keep class com.tatl.fastnote.billing.** { *; }
-keep class com.tatl.fastnote.sync.** { *; }

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