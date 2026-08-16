package com.tatl.fastnote.ui.theme

import androidx.compose.ui.graphics.Color

// ════════════════════════════════════════════════════════════════════
//  Shared App Color Palette — dùng chung cho mọi màn hình
// ════════════════════════════════════════════════════════════════════

// ── Backgrounds ───────────────────────────────────────────────────
/** Nền chính OLED đen tuyệt đối */
val AppBgBlack      = Color(0xFF000000)
/** Nền surface nhẹ hơn (topbar, card) */
val AppBgSurface    = Color(0xFF111111)
/** Nền surface 2 — divider, row */
val AppBgSurface2   = Color(0xFF1A1A1A)
/** Viền / stroke nhẹ */
val AppBorder       = Color(0xFF2E2E2E)

// ── Text ──────────────────────────────────────────────────────────
/** Text chính - trắng */
val AppTextPrimary  = Color(0xFFFFFFFF)
/** Text phụ - xám sáng */
val AppTextSecondary= Color(0xFFAAAAAA)
/** Text mờ - xám đậm */
val AppTextMuted    = Color(0xFF666666)
/** Text hint / disabled */
val AppTextHint     = Color(0xFF444444)

// ── Accent ────────────────────────────────────────────────────────
/** Xanh lá accent chính — FAB, nút xác nhận, highlight */
val AppAccentGreen  = Color(0xFF4CAF50)
/** Xanh lá tối hơn — hover state */
val AppAccentGreenDark = Color(0xFF388E3C)
/** Vàng premium / crown */
val AppGold         = Color(0xFFFFD54F)
/** Đỏ lỗi / hủy */
val AppRed          = Color(0xFFFF5252)
/** Nền đỏ nhạt (badge) */
val AppRedBg        = Color(0x33FF5252)

// ── Note viewer specific ──────────────────────────────────────────
/** Nền note viewer */
val NoteViewerBg    = Color(0xFF0A1A0F)
/** Nền surface note viewer */
val NoteViewerSurface = Color(0xFF142B1A)
/** Text nội dung note */
val NoteTextPrimary = Color(0xFFECF5EE)
/** Text phụ note viewer */
val NoteTextMuted   = Color(0xFF7FAB8A)
