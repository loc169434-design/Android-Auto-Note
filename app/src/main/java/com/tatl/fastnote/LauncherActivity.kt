package com.tatl.fastnote

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.tatl.fastnote.ui.recording.RecordingActivity
import com.tatl.fastnote.util.PinWidgetHelper
import com.tatl.fastnote.util.ThemePreferences
import com.tatl.fastnote.widget.TripleActionWidgetReceiver

/**
 * Trampoline Activity không hiển thị giao diện (Theme.AndroidAutoNote.Launcher).
 * Đóng vai trò làm bộ định tuyến ngay khi người dùng bấm vào App Icon trên màn hình chính:
 *  - Nếu widget đã được ghim & đang hoạt động -> Mở thẳng Mic (RecordingActivity) mà KHÔNG làm chớp HomeScreen.
 *  - Nếu chưa có widget -> Mở HomeScreen (MainActivity) để yêu cầu tạo widget.
 */
class LauncherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kiểm tra thực tế xem widget có đang tồn tại trên màn hình chính của launcher hay không
        val isWidgetActive = PinWidgetHelper.isWidgetActive(this, TripleActionWidgetReceiver::class.java)
        val prefs = getSharedPreferences("auto_note_prefs", Context.MODE_PRIVATE)
        val hasPinned = prefs.getBoolean("has_pinned_widget", false)

        if (isWidgetActive) {
            // Widget đang active trên launcher (kể cả vừa xoá dữ liệu app)
            // -> Đồng bộ lại preference và mở thẳng Mic
            if (!hasPinned) {
                ThemePreferences.setWidgetPinned(true)
            }
            val recordIntent = Intent(this, RecordingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(recordIntent)
        } else {
            // Nếu widget không còn active trên launcher, đồng bộ lại preference
            if (hasPinned) {
                ThemePreferences.setWidgetPinned(false)
            }
            val mainIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(mainIntent)
        }

        finish()
        overridePendingTransition(0, 0)
    }
}
