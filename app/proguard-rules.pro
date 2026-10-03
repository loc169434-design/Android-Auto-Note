# ProGuard / R8 rules for Fast Note (Android Auto Note)
#
# NOTE: Không dùng các rule "keep toàn bộ" (vd: -keep class com.tatl.fastnote.** { *; }
# hoặc -keep class com.google.firebase.** { *; }) vì chúng vô hiệu hoá việc rút gọn,
# làm rối và tối ưu hoá của R8 (Play Console báo tỷ lệ tối ưu hoá thấp ~39%).
# Các thư viện AndroidX / Firebase / Play Services / OkHttp / Billing / Room / WorkManager /
# Glance đều tự đóng gói consumer rules, R8 sẽ tự áp dụng.
# Activity / Service / Receiver / Provider khai báo trong Manifest được AAPT tự giữ lại.

# ── 1. Giữ thông tin dòng code để đọc được stack trace trên Play Console ──
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── 2. Glance ActionCallback — Glance khởi tạo bằng reflection qua tên class ──
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    <init>();
}

# ── 3. WorkManager Worker — WorkManager khởi tạo bằng reflection ──
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ── 4. Credential Manager + Play Services (theo khuyến nghị của Google) ──
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *** *(...);
}

# ── 5. Warnings ──
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**