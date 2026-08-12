# Auto Note — UI Design Guide & AI Prompts

> Tài liệu này mô tả chi tiết UI/UX của app, kèm prompt để dùng AI (Midjourney, DALL-E, Gemini, v.v.) tạo lại UI.

---

## 📱 Tổng quan Design System

### Framework & Style
- **Framework**: Android Jetpack Compose + Material Design 3
- **Theme**: Support Dark/Light mode
- **Font**: System default (Roboto)
- **Corner radius**: 16dp (cards), 28dp (search bar), 24dp (dialog), 20dp (date pill)
- **Elevation**: 2dp (collapsed card), 4dp (expanded card), 8dp (dialog)
- **Icon set**: Material Icons (Filled)

### Color Themes (7 options)
| Theme | Primary Light | Primary Dark | Accent |
|-------|-------------|-------------|--------|
| 🌊 Ocean Blue | #1565C0 | #90CAF9 | #42A5F5 |
| 🌲 Forest Green | #2E7D32 | #A5D6A7 | #66BB6A |
| 🌅 Sunset Orange | #E65100 | #FFCC80 | #FF8F00 |
| 💜 Lavender | #7B1FA2 | #CE93D8 | #AB47BC |
| 🌸 Rose Pink | #C2185B | #F48FB1 | #EC407A |
| 🌙 Midnight | #283593 | #9FA8DA | #5C6BC0 |
| ☕ Coffee | #5D4037 | #BCAAA4 | #8D6E63 |

---

## Màn hình 1: Home — Timeline View

**Preview**: `docs/previews/01_home_timeline.png`

### Prompt để tạo UI:
```
Mobile app UI screenshot, Android Material Design 3, dark mode.
Vietnamese note-taking app "Auto Note" with vertical timeline like Google Calendar.

TOP BAR:
- Left: Calendar icon (blue) + "Auto Note" title (bold, headline)
- Right actions: Document/article icon, sparkle ✨ icon (blue), gear ⚙️ icon

SEARCH BAR (below top bar):
- Full-width rounded pill shape (28dp radius)
- Search 🔍 icon left, placeholder "Tìm kiếm ghi chú..."
- Clear ✕ button when has text
- Background: surfaceVariant 30% opacity, no border when unfocused

TIMELINE CONTENT:
- Date header: Colored pill badge "Hôm nay - DD/MM/YYYY" + horizontal divider line
- Each note entry:
  - LEFT (56dp wide): Time "HH:mm" in primary color, 10dp circle dot, 2dp vertical line
  - RIGHT: Card (16dp corners) with:
    - Row: clock icon 🕐 (14dp) + title (bold, 1 line) + expand ▼ arrow (20dp)
    - 2-line preview text in gray
- Expanded entry: full text + metadata + "Sửa" edit button

FAB: Bottom-right, primary color circle, white microphone 🎤 icon

SPACING: 16dp horizontal padding, 8dp between cards, 4dp between dot and line
```

### Layout specs
```
TopAppBar height: 64dp
SearchBar: margin 16dp horizontal, 8dp vertical, height ~56dp
DateHeader: padding start=16, end=16, top=16, bottom=4
  - Pill: padding horizontal=16, vertical=8
TimelineEntry:
  - Row padding: horizontal 16dp
  - Left column: 56dp wide, center aligned
  - Spacer: 12dp
  - Card: weight 1f, bottom padding 8dp, inner padding 14dp
FAB: default M3 position (16dp from edges)
```

---

## Màn hình 2: Recording — Voice Dialog

**Preview**: `docs/previews/02_recording_screen.png`

### Prompt để tạo UI:
```
Mobile app UI, Material Design 3, dark mode.
Voice recording dialog overlay on semi-transparent black background (50% opacity).

CENTER CARD (90% width, 24dp corners, 8dp elevation):
- HEADER ROW: "Ghi chú nhanh" title left + X close button right
- MIC INDICATOR: 64dp circle, pulsing animation 1→1.3x scale
  - Active: red background 15% opacity, red mic icon
  - Inactive: surfaceVariant background, gray mic icon
- TIMER: HeadlineMedium, red when recording, normal when paused
  Format: "MM:SS" (e.g., "00:12")
- STATUS TEXT: bodySmall, gray
  - "Đang nghe... (tự lưu khi dừng nói)" when active
  - "Tạm dừng" when paused
  - "Đang khởi tạo..." when loading
- TEXT AREA: 150dp height, surfaceVariant background, 12dp corners, 12dp padding
  - Scrollable vertically
  - Italic gray placeholder when empty: "Nội dung sẽ hiển thị ở đây..."
  - Normal text when has content, 22sp line height
- CONTROL BUTTONS (3, evenly spaced):
  1. "Hủy" — 48dp errorContainer circle, X icon, onErrorContainer tint
  2. "Dừng/Tiếp" — 56dp secondaryContainer circle, Pause/Play icon
  3. "Xong" — 48dp primaryContainer circle, Stop ■ icon
  - Each has labelSmall text below

CARD PADDING: 24dp all sides
```

---

## Màn hình 3: Settings — Theme Selector

**Preview**: `docs/previews/03_settings_theme.png`

### Prompt để tạo UI:
```
Mobile app UI, Material Design 3, dark mode. Settings screen.

TOP BAR: Back arrow ← + "Cài đặt" title

SCROLLABLE CONTENT (16dp padding):

CARD 1 — THEME SELECTOR (surfaceVariant 50% opacity, 16dp corners, 20dp padding):
- Header: Palette 🎨 icon (primary color, 24dp) + "Giao diện" title (bold)
- 16dp spacer
- FlowRow (12dp gap both directions) of 7 color circles:
  Each item (clickable Column, center aligned):
    - 48dp circle filled with theme color
    - Selected: 3dp border (onSurface), white ✓ checkmark (24dp) inside
    - Unselected: 1dp border gray 30% opacity
    - 4dp spacer
    - Label: "emoji displayName" in labelSmall
      Selected: primary color, bold
      Unselected: onSurfaceVariant, normal
  
  Colors: #1565C0, #2E7D32, #E65100, #7B1FA2, #C2185B, #283593, #5D4037
  Labels: "🌊 Đại dương", "🌲 Rừng xanh", "🌅 Hoàng hôn", "💜 Lavender", "🌸 Hồng", "🌙 Nửa đêm", "☕ Cà phê"

20dp spacer

CARD 2 — PREMIUM (16dp corners, 20dp padding):
- Title: "Miễn phí" (headlineSmall, bold) or "✨ Premium"
- 8dp spacer
- Body: "Nâng cấp để mở khóa tất cả tính năng."
- 16dp spacer (if not premium)
- Full-width "Nâng cấp Premium" primary button

16dp spacer
- Full-width "Khôi phục giao dịch" outlined button

24dp spacer
- "Auto Note v1.0" bodySmall, onSurfaceVariant
```

---

## Màn hình 4: Inline Edit Mode

**Preview**: `docs/previews/04_edit_mode.png`

### Prompt để tạo UI:
```
Mobile app UI, Material Design 3, dark mode.
Timeline view with one entry in EDIT MODE:

EXPANDED CARD in edit mode:
- Title row: clock icon + title + rotated 180° expand arrow
- OutlinedTextField:
  - Blue focused border (primary color)
  - Editable Vietnamese text content
  - isError=true → red border when empty
  - Supporting text below:
    - Normal: "245 ký tự" (character count) in gray
    - Error (empty): "Nội dung không được để trống" in red
  - MinLines: 3, 12dp corner radius
- Button row (aligned right):
  - "Hủy" TextButton: Close ✕ icon (16dp) + text
  - 8dp spacer
  - "Lưu" TextButton: Check ✓ icon (16dp, primary) + text (primary)
  - Lưu button disabled (gray) when content is blank

SNACKBAR at bottom: "Đã lưu thay đổi" or "⚠️ Lỗi khi lưu: ..."
```

---

## Màn hình 5: Open File — Bottom Sheet

**Preview**: `docs/previews/06_open_file.png`

### Prompt để tạo UI:
```
Mobile app UI, Material Design 3, dark mode.
Modal bottom sheet (skipPartiallyExpanded), surface color, rounded top corners.

HEADER (20dp horizontal padding):
- Left: Document 📄 icon (primary, 24dp) + "Tất cả ghi chú" (titleLarge, bold)
- Right: "24 mục" (labelLarge, onSurfaceVariant)

16dp spacer

SCROLLABLE CONTENT (LazyColumn):
- Date headers: titleSmall, bold, primary color (e.g., "Hôm nay", "Hôm qua")
  - top padding: 16dp (except first)
  - bottom padding: 8dp
- Each entry (Column, fillMaxWidth, bottom padding 12dp):
  - "🕐 HH:mm" — labelMedium, primary, semibold
  - 2dp spacer
  - Content text — bodyMedium, onSurface, 22sp line height
  - 4dp spacer
  - 1dp horizontal divider line (outlineVariant 30% opacity)

Bottom padding: 32dp

Background: dimmed overlay behind sheet
```

---

## Màn hình 6: Widgets (Home Screen)

**Preview**: `docs/previews/05_widgets.png`

### Prompt để tạo UI:
```
Android home screen dark wallpaper with 4 Glance app widgets:

WIDGET 1 — Quick Record (2x1 cells):
- Background: #1565C0 (dark: #0D47A1), 16dp corners
- Center: Mic icon (36dp) + "Ghi chú nhanh" text (13sp, white, medium)
- Click: opens RecordingActivity

WIDGET 2 — Open Notes (2x1 cells):
- Background: #2E7D32 (dark: #1B5E20), 16dp corners
- Center: Agenda icon (32dp) + "📋 Mở ghi chú" (14sp, white, bold)
- Below: "Hôm nay: 3" + "Tổng: 24" (11sp, white 85%/70%)
- Click: opens MainActivity

WIDGET 3 — Recent Notes (4x2 cells):
- Background: white (dark: #1E1E1E), 16dp corners, 12dp padding
- Header row: "📋 Ghi chú gần đây" (14sp, blue, bold) + "+ Ghi" blue pill button
- 3-5 note rows: gray background #F5F5F5 (dark: #2C2C2C), 8dp corners, 8dp padding
  Each: title (12sp, medium, 1 line) + time right (11sp, gray)
- Empty state: "Chưa có ghi chú" centered gray text

WIDGET 4 — Stats (2x1 cells):
- Shows total count and today count with chart icon
```

---

## 🔄 User Flows

### Flow 1: Ghi âm → Tự động lưu
```
Widget/FAB 🎤 → RecordingActivity → Mic ON → Nói...
  ├─ Nói liên tục → Timer reset mỗi lần nghe giọng
  ├─ Dừng > 15s → Auto-stop → Auto-save → Toast ✅ → Close
  ├─ Bấm "Xong" ■ → Auto-save → Toast ✅ → Close  
  └─ Bấm "Hủy" ✕ → Discard → Close
```

### Flow 2: Xem & Sửa note
```
HomeScreen → Tap entry → Expand (accordion)
  ├─ Đọc full content + metadata
  ├─ Bấm "Sửa" ✏️ → TextField editable
  │   ├─ Sửa → "Lưu" ✓ → Snackbar "Đã lưu" → Read mode
  │   └─ Sai → "Hủy" ✕ → Revert → Read mode
  └─ Tap lại → Collapse
```

### Flow 3: Đổi theme
```
Home → ⚙️ Settings → Theme card → Tap color circle
  → Instant apply (StateFlow reactive)
  → Auto-save SharedPreferences
  → Persist across app restart
```

### Flow 4: Mở file
```
Home → 📄 icon → BottomSheet "Tất cả ghi chú"
  → Scroll document liên tục (nhóm theo ngày)
  → Swipe down to dismiss
```

### Flow 5: AI Share
```
Home → ✨ icon → AIShareHelper:
  ├─ Detect Gemini/ChatGPT/Copilot/Claude
  ├─ Format today's notes + prompt "Hãy tóm tắt..."
  ├─ Có AI app → Open directly
  └─ Không có → Share chooser
```

---

## 📐 Spacing & Typography Reference

| Element | Style | Size |
|---------|-------|------|
| App title | headlineSmall | Bold |
| Date header pill | labelLarge | SemiBold |
| Note title | titleSmall | SemiBold |
| Note content | bodyMedium | Normal, 22sp line |
| Preview text | bodySmall | Normal, 18sp line |
| Time text | labelMedium | Medium, 13sp |
| Metadata | labelSmall | Normal, 60% alpha |
| Button label | labelSmall | Normal |
| Search placeholder | bodyMedium | Normal |
| Dialog title | titleMedium | Normal |
| Timer | headlineMedium | Normal |
| Sheet title | titleLarge | Bold |

---

## 🎨 Dark Mode Surfaces

| Element | Light | Dark |
|---------|-------|------|
| Background | #FFFBFE | #1C1B1F |
| Surface | #FFFBFE | #1C1B1F |
| SurfaceVariant | #E7E0EC | #49454F |
| Card (collapsed) | surface | surface |
| Card (expanded) | primaryContainer 15% | primaryContainer 15% |
| Search bar bg | surfaceVariant 30% | surfaceVariant 30% |
| Text area bg | surfaceVariant | surfaceVariant |
| Divider | outlineVariant | outlineVariant |
| Timeline line | outlineVariant 50% | outlineVariant 50% |
