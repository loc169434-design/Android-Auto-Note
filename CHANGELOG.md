# Android Auto Note — Changelog

## Tổng quan dự án
Ứng dụng ghi chú tự động bằng giọng nói cho Android, sử dụng Jetpack Compose, Room DB, Jetpack Glance (Widgets), và VoiceRecordingService (Foreground Service).

---

## Phiên bản hiện tại: v1.1.0

### Ngày cập nhật: 2026-08-05

---

## Các tính năng đã implement

### 🎤 Voice-to-Text — Pause/Resume/Auto-Save
**Files:**
- `app/src/main/java/.../service/VoiceRecordingService.kt`
- `app/src/main/java/.../ui/recording/RecordingActivity.kt`
- `app/src/main/java/.../ui/recording/RecordingScreen.kt`

**Chi tiết:**
- Nút Pause/Resume hoạt động: gửi `ACTION_PAUSE` / `ACTION_RESUME` intent tới Service
- **Auto-stop**: Mic tự tắt sau 15 giây không phát hiện giọng nói (`IDLE_TIMEOUT_MS = 15_000`)
- **Auto-save**: Khi mic tự tắt hoặc user bấm Stop → tự động lưu note vào Room DB
- Không cần bấm Save thủ công — nút Save đã được thay bằng nút Stop (Xong)
- Không chạy background — service dừng hoàn toàn sau khi lưu
- `autoSaveTriggered: StateFlow<Boolean>` — signal từ Service → Activity để trigger auto-save
- `resetIdleTimeout()` — reset timer mỗi khi phát hiện giọng nói mới
- UI: 3 nút điều khiển — Hủy (discard) / Dừng-Tiếp (pause/resume) / Xong (stop & auto-save)

---

### 📤 Share — Fix crash
**File:** `app/src/main/java/.../ui/detail/DetailScreen.kt`

**Chi tiết:**
- Thêm `FLAG_ACTIVITY_NEW_TASK` vào chooser Intent
- Wrap trong `try-catch` → hiển thị Toast khi lỗi thay vì crash

---

### 📅 HomeScreen — Calendar Timeline + Accordion + Search
**Files:**
- `app/src/main/java/.../ui/home/HomeScreen.kt`
- `app/src/main/java/.../ui/home/HomeViewModel.kt`
- `app/src/main/java/.../util/DateUtils.kt`

**Chi tiết:**

#### Timeline UI (kiểu Google Calendar)
- Notes nhóm theo ngày với date pill headers ("Hôm nay", "Hôm qua", "Thứ Hai, 10/08/2026")
- Mỗi note hiển thị: giờ bên trái + dot + đường timeline dọc + card bên phải
- Swipe-to-dismiss để xóa note

#### Accordion Expand
- Bấm vào entry → expand tại chỗ (không chuyển trang)
- Collapsed: title + 2 dòng preview
- Expanded: full content + metadata (tạo lúc, sửa lúc)
- Animation mượt (expandVertically/shrinkVertically)

#### Inline Edit
- Trong expanded mode, bấm "Sửa" → chuyển sang TextField editable
- Nút "Lưu" (save vào DB) hoặc "Hủy" (revert)
- Khi thu gọn accordion → tự động thoát edit mode

#### Input Validation & Error Handling
- Giới hạn input: max 50.000 ký tự (tránh OOM)
- Không cho lưu nội dung trống: TextField viền đỏ + text "Nội dung không được để trống"
- Nút Lưu disabled khi nội dung trống
- try-catch quanh DB save/delete operations
- Snackbar feedback: "Đã lưu thay đổi" (success) / "⚠️ Lỗi khi lưu: ..." (error)
- `EditFeedback` sealed class: None / Success / Error

#### Search
- Search bar ngay dưới TopAppBar
- Full-text search qua title + content (`WHERE title LIKE '%query%' OR content LIKE '%query%'`)
- Filter realtime — date headers tự adjust theo kết quả
- Clear (✕) button → hiện lại tất cả
- Empty state: "Không tìm thấy ghi chú cho 'query'"

#### Open File (Mở file)
- Nút 📄 trên TopAppBar
- Mở BottomSheet hiển thị TẤT CẢ ghi chú dạng document liên tục
- Nhóm theo ngày, mỗi entry: 🕐 HH:mm + nội dung

---

### ✨ AI Share
**Files:**
- `app/src/main/java/.../util/AIShareHelper.kt` (MỚI)
- `app/src/main/java/.../MainActivity.kt`

**Chi tiết:**
- Nút ✨ trên TopAppBar → gửi tất cả ghi chú hôm nay tới AI
- Detect AI apps đã cài: Gemini, ChatGPT, Copilot, Claude
- Nếu có app AI → mở trực tiếp
- Nếu không → mở share chooser chung
- Prompt tự động: "Hãy tóm tắt những ghi chú trong ngày hôm nay của tôi..."
- Export format: 🕐 HH:mm + nội dung cho mỗi entry

---

### 🎨 Theme System (7 màu)
**Files:**
- `app/src/main/java/.../util/ThemePreferences.kt` (MỚI)
- `app/src/main/java/.../ui/theme/Color.kt`
- `app/src/main/java/.../ui/theme/Theme.kt`
- `app/src/main/java/.../ui/settings/SettingsScreen.kt`
- `app/src/main/java/.../AutoNoteApplication.kt`

**Chi tiết:**
- 7 color themes, mỗi theme có light + dark variant:
  | Theme        | Emoji | Tên hiển thị |
  |-------------|-------|-------------|
  | OCEAN_BLUE  | 🌊    | Đại dương   |
  | FOREST_GREEN| 🌲    | Rừng xanh   |
  | SUNSET_ORANGE| 🌅   | Hoàng hôn   |
  | LAVENDER    | 💜    | Lavender    |
  | ROSE_PINK   | 🌸    | Hồng        |
  | MIDNIGHT    | 🌙    | Nửa đêm     |
  | COFFEE      | ☕    | Cà phê      |

- Lưu vào SharedPreferences (`auto_note_prefs` / `selected_theme`)
- Khôi phục khi mở app (`ThemePreferences.init()` trong `AutoNoteApplication.onCreate()`)
- Theme selector trong Settings: FlowRow 7 color circles + check mark
- Apply ngay lập tức qua `StateFlow` reactive

---

### 📱 Widgets (4 widgets)
**Files:**
- `app/src/main/java/.../widget/QuickRecordWidget.kt` (có sẵn)
- `app/src/main/java/.../widget/RecentNotesWidget.kt` (sửa)
- `app/src/main/java/.../widget/OpenNotesWidget.kt` (MỚI)
- `app/src/main/java/.../widget/WidgetUpdater.kt` (sửa)
- `app/src/main/res/xml/open_notes_widget_info.xml` (MỚI)
- `app/src/main/AndroidManifest.xml` (sửa)

**Chi tiết:**
| Widget | Mô tả | Click → |
|--------|-------|---------|
| 🎤 Quick Record | Nút ghi âm nhanh (xanh dương) | RecordingActivity |
| 📋 Open Notes (MỚI) | "Hôm nay: X / Tổng: Y" (xanh lá) | MainActivity |
| 📝 Recent Notes | 5 note mới nhất | MainActivity |
| 📊 Stats | Thống kê tổng số note | — |

---

### 🧹 Navigation đơn giản hóa
**File:** `app/src/main/java/.../MainActivity.kt`

**Chi tiết:**
- Bỏ route `detail/{noteId}` — không cần trang detail riêng
- Mọi thứ trên 1 màn hình timeline với accordion expand
- Chỉ còn 2 routes: `home` và `settings`

---

## Data Layer
**Files:**
- `app/src/main/java/.../data/db/NoteDao.kt`
- `app/src/main/java/.../data/repository/NoteRepository.kt`

**Queries đã thêm:**
- `searchNotes(query)` — full-text search title + content
- `getNotesToday(startOfDay)` — lấy notes hôm nay cho AI share
- `exportTodayAsText()` — format text cho AI consumption

---

## Utility files
| File | Mô tả |
|------|-------|
| `util/DateUtils.kt` | formatTimeOnly, formatDayHeader, formatRelativeDay, getDateKey, isSameDay |
| `util/AIShareHelper.kt` | Detect AI apps, create share intent, launch AI share |
| `util/ThemePreferences.kt` | SharedPreferences manager, AppTheme enum |

---

## Cấu trúc file chính
```
app/src/main/java/com/example/androidautonote/
├── AutoNoteApplication.kt          — App init + ThemePreferences
├── MainActivity.kt                 — Navigation (home, settings)
├── data/
│   ├── db/
│   │   ├── NoteDao.kt              — Room queries (search, today)
│   │   ├── NoteEntity.kt           — Entity
│   │   └── AppDatabase.kt          — Room DB
│   └── repository/
│       └── NoteRepository.kt       — Data access + export
├── service/
│   └── VoiceRecordingService.kt    — Speech recognition + auto-stop
├── ui/
│   ├── home/
│   │   ├── HomeScreen.kt           — Timeline + Accordion + Search + Edit
│   │   └── HomeViewModel.kt        — State management + validation
│   ├── recording/
│   │   ├── RecordingActivity.kt    — Auto-save observer
│   │   └── RecordingScreen.kt      — Recording UI (no Save button)
│   ├── detail/
│   │   ├── DetailScreen.kt         — (giữ nguyên, không dùng)
│   │   └── DetailViewModel.kt      — (giữ nguyên, không dùng)
│   ├── settings/
│   │   └── SettingsScreen.kt       — Theme selector + Premium
│   └── theme/
│       ├── Color.kt                — 7 color palettes
│       ├── Theme.kt                — Multi-theme support
│       └── Type.kt                 — Typography
├── util/
│   ├── AIShareHelper.kt            — AI app detection + share
│   ├── DateUtils.kt                — Date/time formatting
│   ├── FileExportHelper.kt         — Export .txt
│   └── ThemePreferences.kt         — SharedPreferences theme
└── widget/
    ├── QuickRecordWidget.kt        — Widget ghi âm
    ├── RecentNotesWidget.kt        — Widget recent notes
    ├── OpenNotesWidget.kt          — Widget mở file
    ├── StatsWidget.kt              — Widget thống kê
    └── WidgetUpdater.kt            — Refresh all widgets
```

---

## Build Status
✅ BUILD SUCCESSFUL — 0 errors, 0 warnings
