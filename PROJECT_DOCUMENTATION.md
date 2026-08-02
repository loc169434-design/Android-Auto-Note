# AndroidAutoNote — Tài liệu dự án & Tiến độ

> **Cập nhật lần cuối:** 2026-08-02  
> **Trạng thái build:** ✅ BUILD SUCCESSFUL (0 errors, 0 warnings)  
> **Phiên bản:** 1.0 PoC  

---

## 1. Tổng quan dự án

**AndroidAutoNote** là ứng dụng tiện ích cá nhân trên Android, tập trung tối ưu trải nghiệm **tốc ký bằng giọng nói** qua **Widget ngoài màn hình chính**.

### Yêu cầu kỹ thuật chính (4 bài toán)

| # | Bài toán | Trạng thái |
|---|----------|-----------|
| 1 | Hộp thoại thu âm nổi từ Widget (Foreground Service Microphone, không tự ngắt khi ngập ngừng) | ✅ Đã implement |
| 2 | Hệ thống 3 Widget ngoài màn hình chính | ✅ Đã implement |
| 3 | Quản lý lưu trữ & Xuất file .txt | ✅ Đã implement |
| 4 | Cổng thanh toán Google Play Billing (one-time purchase) | ✅ Đã implement |

### Tech Stack

| Thành phần | Công nghệ | Phiên bản |
|-----------|-----------|-----------|
| Ngôn ngữ | Kotlin | 2.0.21 |
| UI | Jetpack Compose + Material3 | BOM 2024.12.01 |
| Database | Room | 2.7.1 |
| Widget | Jetpack Glance | 1.1.1 |
| Navigation | Navigation Compose | 2.9.0 |
| Billing | Google Play Billing | 7.1.1 |
| Annotation Processing | KSP | 2.0.21-1.0.28 |
| Build | AGP | 8.13.2 |
| Target SDK | 35 (Android 15) | |
| Min SDK | 26 (Android 8.0) | |
| Compile SDK | 36 | |

---

## 2. Kiến trúc hệ thống

### 2.1 Sơ đồ tổng quan

```
┌─────────────────────────────────────────────────────────┐
│                   HOME SCREEN WIDGETS                    │
│  ┌──────────────┐ ┌──────────────────┐ ┌──────────────┐ │
│  │ 🎤 Quick     │ │ 📋 Recent Notes  │ │ 📊 Stats     │ │
│  │   Record     │ │   (5 gần nhất)   │ │ (tổng+today) │ │
│  └──────┬───────┘ └────────┬─────────┘ └──────┬───────┘ │
└─────────┼──────────────────┼───────────────────┼─────────┘
          │ PendingIntent    │ read              │ read
          ▼                  ▼                   ▼
┌─────────────────┐   ┌──────────────────────────────┐
│ RecordingActivity│   │        Room Database          │
│ (Transparent    │──▶│  NoteEntity (id, title,       │
│  Dialog)        │   │  content, createdAt, updatedAt)│
└────────┬────────┘   └──────────────┬─────────────────┘
         │ start/bind                │
         ▼                           │
┌─────────────────────┐              │
│ VoiceRecordingService│              │
│ (Foreground Service  │              │
│  type=microphone)    │              │
│                      │              │
│  ┌─────────────────┐ │              │
│  │ SpeechRecognizer │ │    ┌────────┴────────┐
│  │ (auto-restart    │ │    │  MainActivity    │
│  │  on silence)     │ │    │  ├─ HomeScreen   │
│  └─────────────────┘ │    │  ├─ DetailScreen  │
└──────────────────────┘    │  └─ SettingsScreen│
                             └────────┬─────────┘
                                      │
                          ┌───────────┼───────────┐
                          │           │           │
                     FileProvider  BillingMgr   NavHost
                     (.txt export) (one-time)
```

### 2.2 Package structure

```
com.example.androidautonote/
├── AutoNoteApplication.kt          # Application singleton (DB + Repository)
├── MainActivity.kt                 # NavHost: home → detail → settings
│
├── data/
│   ├── db/
│   │   ├── NoteEntity.kt           # Room @Entity
│   │   ├── NoteDao.kt              # Room @Dao (CRUD + widget queries)
│   │   └── AppDatabase.kt          # Room Database singleton
│   └── repository/
│       └── NoteRepository.kt       # Repository wrapper
│
├── service/
│   └── VoiceRecordingService.kt    # Foreground Service + SpeechRecognizer
│
├── ui/
│   ├── recording/
│   │   ├── RecordingActivity.kt    # Transparent Activity (hộp thoại nổi)
│   │   └── RecordingScreen.kt      # Compose UI (animation, real-time text)
│   ├── home/
│   │   ├── HomeScreen.kt           # Danh sách note, swipe-to-delete
│   │   └── HomeViewModel.kt        # ViewModel
│   ├── detail/
│   │   ├── DetailScreen.kt         # Xem/sửa note, chia sẻ, xóa
│   │   └── DetailViewModel.kt      # ViewModel
│   ├── settings/
│   │   └── SettingsScreen.kt       # Premium status, upgrade button
│   └── theme/                      # Material3 theme (auto-generated)
│
├── widget/
│   ├── QuickRecordWidget.kt        # Widget 1: Nút ghi âm nhanh (2x1)
│   ├── RecentNotesWidget.kt        # Widget 2: 5 note gần nhất (4x3)
│   ├── StatsWidget.kt              # Widget 3: Thống kê (3x2)
│   └── WidgetUpdater.kt            # Helper refresh tất cả widget
│
├── billing/
│   └── BillingManager.kt           # Google Play Billing one-time purchase
│
└── util/
    ├── DateUtils.kt                # Format date/time/duration
    └── FileExportHelper.kt         # Tạo .txt + share Intent via FileProvider
```

### 2.3 Resources

```
res/
├── layout/
│   └── widget_placeholder.xml      # Placeholder cho Glance widget
├── values/
│   ├── strings.xml                 # Tất cả string tiếng Việt
│   ├── themes.xml                  # Theme chính + Transparent theme
│   └── colors.xml                  # Color palette
└── xml/
    ├── file_paths.xml              # FileProvider paths config
    ├── quick_record_widget_info.xml
    ├── recent_notes_widget_info.xml
    ├── stats_widget_info.xml
    ├── backup_rules.xml
    └── data_extraction_rules.xml
```

---

## 3. Chi tiết kỹ thuật từng module

### 3.1 VoiceRecordingService — Foreground Service Microphone

**File:** `service/VoiceRecordingService.kt`

**Vấn đề giải quyết:** Android 14/15 có chính sách bảo mật nghiêm ngặt:
- Cấm khởi chạy Activity/Service trực tiếp từ nền
- Foreground Service bắt buộc khai báo `foregroundServiceType` tại runtime
- `SpeechRecognizer` mặc định tự ngắt khi im lặng 3-5 giây

**Giải pháp đã implement:**

1. **Android 14+ Compliance:**
   - Khai báo `android:foregroundServiceType="microphone"` trong Manifest
   - Gọi `startForeground()` với `ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE` tại runtime (API 34+)
   - Service luôn được khởi chạy từ `RecordingActivity` (visible Activity) — không từ nền

2. **Auto-restart SpeechRecognizer (chống ngắt khi ngập ngừng):**
   ```
   onResults() → lưu text vào buffer → tự restart SpeechRecognizer sau 200ms
   onError(NO_MATCH/SPEECH_TIMEOUT) → tự restart sau 300ms
   onError(RECOGNIZER_BUSY) → tự restart sau 1000ms
   onError(khác) → tự restart sau 2000ms
   ```
   - Text được tích lũy trong `StringBuilder` qua các lần restart
   - Cờ `shouldKeepListening` kiểm soát vòng lặp restart
   - Chỉ dừng thực sự khi user bấm Stop

3. **State management qua StateFlow:**
   - `recognizedText: StateFlow<String>` — text đã xác nhận
   - `partialText: StateFlow<String>` — text đang nhận diện (real-time)
   - `isListening: StateFlow<Boolean>` — trạng thái đang nghe
   - `isPaused: StateFlow<Boolean>` — trạng thái tạm dừng
   - `recordingSeconds: StateFlow<Int>` — thời gian đã thu

4. **Ngôn ngữ:** Mặc định `vi-VN` (tiếng Việt), cấu hình trong `createRecognizerIntent()`

### 3.2 RecordingActivity — Hộp thoại nổi từ Widget

**Files:** `ui/recording/RecordingActivity.kt`, `ui/recording/RecordingScreen.kt`

**Cơ chế:**
- Activity sử dụng theme `Theme.AndroidAutoNote.Transparent` → hiển thị như dialog nổi
- `excludeFromRecents="true"` + `taskAffinity=""` → không hiện trong recent apps
- Bind tới `VoiceRecordingService` qua `ServiceConnection` để observe state
- Xin quyền `RECORD_AUDIO` + `POST_NOTIFICATIONS` tại runtime

**UI (Compose):**
- Pulse animation trên icon micro khi đang thu
- Text real-time hiển thị trong scroll box
- Timer đếm ngược
- 3 nút: Cancel (❌) / Pause-Resume (⏸️▶️) / Save (💾)
- Auto-generate title từ 10 từ đầu tiên

### 3.3 Widgets — Jetpack Glance

**Files:** `widget/QuickRecordWidget.kt`, `widget/RecentNotesWidget.kt`, `widget/StatsWidget.kt`

| Widget | Kích thước | Chức năng | Auto-update |
|--------|-----------|-----------|------------|
| Quick Record | 2x1 | Nút bấm → mở RecordingActivity | Không (static) |
| Recent Notes | 4x3 | Hiển thị 5 note gần nhất + nút "+ Ghi" | 30 phút + sau mỗi lần save |
| Stats | 3x2 | Tổng số note + số note hôm nay | 30 phút + sau mỗi lần save |

- Tất cả đọc data trực tiếp từ Room DB trong `provideGlance()`
- `WidgetUpdater.updateAllWidgets()` được gọi sau mỗi lần save note
- Hỗ trợ Dark/Light mode qua `ColorProvider(day=..., night=...)`

### 3.4 Room Database

**Files:** `data/db/NoteEntity.kt`, `data/db/NoteDao.kt`, `data/db/AppDatabase.kt`

```kotlin
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

**NoteDao queries:**
- `getAllNotes()` — Flow<List>, order by createdAt DESC
- `getNoteById()` — Flow<NoteEntity?>
- `getRecentNotes(limit)` — cho widget Recent Notes
- `getNoteCount()` — cho widget Stats
- `getNotesCountToday(startOfDay)` — cho widget Stats
- `insert()`, `update()`, `delete()`, `deleteById()`

### 3.5 FileProvider & Export

**File:** `util/FileExportHelper.kt`

- Tạo file `.txt` trong `cacheDir/exports/` (không cần quyền Storage)
- Nội dung file: Title + Ngày tạo + Nội dung note
- Chia sẻ qua `Intent.ACTION_SEND` với `FileProvider.getUriForFile()`
- Đã khai báo `FileProvider` trong Manifest + `file_paths.xml`

### 3.6 Google Play Billing

**File:** `billing/BillingManager.kt`

**Đã implement:**
- `initialize()` — kết nối BillingClient
- `queryProductDetails()` — lấy thông tin sản phẩm `premium_unlock`
- `queryExistingPurchases()` — kiểm tra/khôi phục giao dịch đã mua
- `launchPurchaseFlow(activity)` — mở dialog thanh toán
- `handlePurchase()` — xử lý kết quả mua
- `acknowledgePurchase()` — xác nhận giao dịch (bắt buộc trong 3 ngày)
- `isPremium: StateFlow<Boolean>` — trạng thái premium reactive
- `getPriceString()` — lấy giá hiển thị

**Lưu ý PoC:** Xác thực giao dịch cục bộ (local). Production cần server-side verify.

### 3.7 Màn hình chính

**Navigation:** `NavHost` với 3 destinations:
- `"home"` → HomeScreen (danh sách note)
- `"detail/{noteId}"` → DetailScreen (xem/sửa)
- `"settings"` → SettingsScreen (premium)

**HomeScreen:**
- `LazyColumn` với note items (title, ngày, preview 2 dòng)
- Swipe-to-delete với dialog xác nhận
- Empty state khi chưa có note
- FAB micro → mở RecordingActivity

**DetailScreen:**
- Editable title + content (OutlinedTextField)
- Nút Share → FileExportHelper → Intent chooser
- Nút Save → update Room DB
- Nút Delete → dialog xác nhận → pop back

**SettingsScreen:**
- Card hiển thị trạng thái Free/Premium
- Nút "Nâng cấp Premium" / "Khôi phục giao dịch"

---

## 4. Permissions đã khai báo

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="com.android.vending.BILLING" />
```

> ⚠️ **Lưu ý Google Play Console:** Quyền `FOREGROUND_SERVICE_MICROPHONE` cần nộp Declaration Form kèm video demo giải thích tại sao app cần thu âm từ nền.

---

## 5. Tiến độ chi tiết

### ✅ Đã hoàn thành (Ngày 1-6)

| Ngày | Công việc | Files |
|------|-----------|-------|
| **1** | Setup dependencies (Room, Glance, Navigation, Billing, KSP), package structure, Room Database (Entity, DAO, Database, Repository), Application class, DateUtils, FileExportHelper, AndroidManifest, string resources, themes | 14 files |
| **2** | VoiceRecordingService (Foreground Service + SpeechRecognizer auto-restart), Notification Channel | 1 file |
| **3** | RecordingActivity (Transparent Dialog), RecordingScreen (Compose UI, pulse animation, real-time text, controls) | 2 files |
| **4** | 3 Widgets (QuickRecord, RecentNotes, Stats) bằng Jetpack Glance, WidgetUpdater, widget info XMLs | 4 files + 3 XMLs |
| **5** | HomeScreen + ViewModel, DetailScreen + ViewModel, SettingsScreen, MainActivity với NavHost | 6 files |
| **6** | BillingManager (initialize, purchase, acknowledge, restore) | 1 file |

**Tổng: ~28 files đã tạo/sửa. Build status: ✅ SUCCESSFUL**

### ❌ Chưa làm (Ngày 7 — Polish & Store)

| Hạng mục | Chi tiết | Độ ưu tiên |
|----------|---------|-----------|
| Kết nối BillingManager vào UI | Inject BillingManager vào AutoNoteApplication, kết nối SettingsScreen với launchPurchaseFlow() | Cao |
| Đổi applicationId | `com.example.androidautonote` → tên chính thức của khách | **Bắt buộc trước khi lên Store** |
| Tạo product ID trên Play Console | Tạo in-app product `premium_unlock` trên Google Play Console | Cao |
| App icon | Thay icon mặc định bằng icon chính thức | Trung bình |
| ProGuard/R8 | Bật `isMinifyEnabled = true`, thêm rules cho Room, Billing, Glance | Cao |
| Signing keystore | Tạo release keystore cho production | **Bắt buộc** |
| Privacy Policy | Tạo trang Privacy Policy (bắt buộc cho app dùng Microphone) | **Bắt buộc** |
| Screenshots & Video demo | Ít nhất 2 screenshots + video demo thu âm cho Declaration Form | **Bắt buộc** |
| Test thiết bị thật | Test trên Android 14/15 thực tế, đặc biệt luồng Widget → Thu âm → Save | Cao |
| Edge cases UI | Loading states, error handling toàn diện, keyboard handling trên DetailScreen | Trung bình |

---

## 6. Hướng dẫn build & chạy

### 6.1 Yêu cầu môi trường
- Android Studio Ladybug trở lên
- JDK 11+
- Android SDK 36
- Thiết bị/Emulator API 26+ (khuyến nghị API 34+ để test Foreground Service)

### 6.2 Build debug
```bash
cd /path/to/AndroidAutoNote
./gradlew assembleDebug
```

### 6.3 Build release (sau khi có keystore)
```bash
./gradlew assembleRelease
# hoặc
./gradlew bundleRelease  # AAB cho Google Play
```

### 6.4 Chạy trên thiết bị
```bash
./gradlew installDebug
adb shell am start -n com.example.androidautonote/.MainActivity
```

---

## 7. Lưu ý quan trọng cho developer tiếp nhận

### 7.1 Trước khi code tiếp
1. Mở project trong Android Studio → **Sync Gradle** → đảm bảo không lỗi
2. Chạy `./gradlew assembleDebug` → phải **BUILD SUCCESSFUL**
3. Đọc kỹ `VoiceRecordingService.kt` — đây là file phức tạp nhất

### 7.2 Những điểm cần lưu ý
- **SpeechRecognizer chạy trên Main Thread** — đây là yêu cầu của Android API, không được chuyển sang background thread
- **Widget Glance dùng `provideGlance()` suspend** — có thể query Room DB trực tiếp
- **BillingManager chưa được inject vào UI** — cần kết nối vào `AutoNoteApplication` và `SettingsScreen`
- **applicationId `com.example.*`** — PHẢI đổi trước khi lên Store
- **Chưa có unit test** — cần thêm test cho NoteRepository và BillingManager

### 7.3 Cách test thu âm trên Emulator
- Emulator hỗ trợ thu âm qua mic máy tính host
- Cần cấp quyền `RECORD_AUDIO` thủ công trong Settings nếu dialog không hiện
- Trên emulator API 34+, phải chạy app ít nhất 1 lần trước khi test Widget

### 7.4 Quy trình xin duyệt quyền Microphone trên Play Console
1. Upload AAB lên **Internal Testing** track
2. Vào **Policy and programs** → **App content** → **Sensitive permissions**
3. Khai báo `FOREGROUND_SERVICE_MICROPHONE`:
   - Mô tả: "App sử dụng Foreground Service loại Microphone để thu âm giọng nói và chuyển thành văn bản theo thời gian thực. Người dùng chủ động bấm Widget/nút để bắt đầu thu âm."
   - Đính kèm video demo quay lại toàn bộ luồng: bấm Widget → dialog thu âm hiện → nói → text hiển thị → bấm Save
4. Chờ Google review (thường 1-3 ngày làm việc)

---

## 8. Danh sách file đầy đủ

```
AndroidAutoNote/
├── build.gradle.kts                                          # Root: KSP plugin
├── settings.gradle.kts
├── gradle/
│   └── libs.versions.toml                                    # Version catalog
├── app/
│   ├── build.gradle.kts                                      # Dependencies
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml                               # Permissions, components
│       ├── java/com/example/androidautonote/
│       │   ├── AutoNoteApplication.kt
│       │   ├── MainActivity.kt
│       │   ├── billing/
│       │   │   └── BillingManager.kt
│       │   ├── data/
│       │   │   ├── db/
│       │   │   │   ├── AppDatabase.kt
│       │   │   │   ├── NoteDao.kt
│       │   │   │   └── NoteEntity.kt
│       │   │   └── repository/
│       │   │       └── NoteRepository.kt
│       │   ├── service/
│       │   │   └── VoiceRecordingService.kt
│       │   ├── ui/
│       │   │   ├── detail/
│       │   │   │   ├── DetailScreen.kt
│       │   │   │   └── DetailViewModel.kt
│       │   │   ├── home/
│       │   │   │   ├── HomeScreen.kt
│       │   │   │   └── HomeViewModel.kt
│       │   │   ├── recording/
│       │   │   │   ├── RecordingActivity.kt
│       │   │   │   └── RecordingScreen.kt
│       │   │   ├── settings/
│       │   │   │   └── SettingsScreen.kt
│       │   │   └── theme/
│       │   │       ├── Color.kt
│       │   │       ├── Theme.kt
│       │   │       └── Type.kt
│       │   ├── util/
│       │   │   ├── DateUtils.kt
│       │   │   └── FileExportHelper.kt
│       │   └── widget/
│       │       ├── QuickRecordWidget.kt
│       │       ├── RecentNotesWidget.kt
│       │       ├── StatsWidget.kt
│       │       └── WidgetUpdater.kt
│       └── res/
│           ├── layout/
│           │   └── widget_placeholder.xml
│           ├── values/
│           │   ├── colors.xml
│           │   ├── strings.xml
│           │   └── themes.xml
│           └── xml/
│               ├── backup_rules.xml
│               ├── data_extraction_rules.xml
│               ├── file_paths.xml
│               ├── quick_record_widget_info.xml
│               ├── recent_notes_widget_info.xml
│               └── stats_widget_info.xml
```

---

*Tài liệu này được tạo để phục vụ báo cáo khách hàng và bàn giao kỹ thuật. Đặt tại thư mục gốc dự án để bất kỳ agent/developer nào mở project đều có thể đọc được.*
