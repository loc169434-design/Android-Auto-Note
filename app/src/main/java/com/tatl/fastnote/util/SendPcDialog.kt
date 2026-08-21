package com.tatl.fastnote.util

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tatl.fastnote.R
import com.tatl.fastnote.ui.theme.InterFontFamily
import com.tatl.fastnote.ui.theme.NotoSansFontFamily

// ── Bảng màu chuẩn Slate-Blue ────────────────────────────────────────────────
private val DialogBgTop      = Color(0xFF1C2D3D)
private val DialogBgBottom   = Color(0xFF101B26)
private val DialogBorder     = Color(0xFF2E465E)
private val InputFieldBg     = Color(0xFF0B141C)
private val InputBorder      = Color(0xFF334B63)
private val InputBorderFocus = Color(0xFF3B82F6)
private val TextTitle        = Color(0xFFF1F5F9)
private val TextMuted        = Color(0xFF94A3B8)
private val TextError        = Color(0xFFFF5252)

/**
 * Popup "Gửi PC" — nhập mật khẩu để nén AES-256 với giao diện Slate-Blue hiện đại
 * và nút con mắt bật/tắt xem mật khẩu.
 */
@Composable
fun SendPcDialog(
    onDismiss: () -> Unit,
    onConfirm: (password: String) -> Unit
) {
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMsg        by remember { mutableStateOf("") }

    fun submit() {
        when {
            password.length < 6 ->
                errorMsg = "Mật khẩu tối thiểu 6 ký tự"
            !password.all { it.isLetterOrDigit() && it.code < 128 } ->
                errorMsg = "Chỉ dùng chữ và số không dấu (a-z, A-Z, 0-9)"
            else -> onConfirm(password)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f),
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, DialogBorder),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(DialogBgTop, DialogBgBottom)
                        )
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Icon khóa bảo mật ─────────────────────────────────────────
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E3A8A).copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.6f)),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Bảo mật",
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Tiêu đề ───────────────────────────────────────────────────
                Text(
                    text = stringResource(R.string.str_send_pc_title),
                    color = TextTitle,
                    fontSize = 18.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Mật khẩu dùng để mã hóa file nén AES-256 khi truyền sang máy tính",
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontFamily = NotoSansFontFamily,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(20.dp))

                // ── Ô nhập mật khẩu với icon con mắt ───────────────────────────
                OutlinedTextField(
                    value = password,
                    onValueChange = { raw ->
                        val filtered = raw.filter { it.isLetterOrDigit() && it.code < 128 }
                        password = filtered
                        errorMsg = ""
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submit() }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                                tint = if (passwordVisible) Color(0xFF60A5FA) else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor        = TextTitle,
                        unfocusedTextColor      = TextTitle,
                        focusedContainerColor   = InputFieldBg,
                        unfocusedContainerColor = InputFieldBg,
                        focusedBorderColor      = InputBorderFocus,
                        unfocusedBorderColor    = InputBorder,
                        cursorColor             = Color.White
                    ),
                    textStyle = TextStyle(
                        fontFamily = NotoSansFontFamily,
                        fontSize = 16.sp,
                        letterSpacing = if (passwordVisible) 0.5.sp else 2.sp
                    ),
                    placeholder = {
                        Text(
                            text = "Nhập mật khẩu (tối thiểu 6 ký tự)",
                            color = TextMuted.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            fontFamily = NotoSansFontFamily
                        )
                    }
                )

                // ── Lỗi validation ────────────────────────────────────────────
                if (errorMsg.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorMsg,
                        color = TextError,
                        fontSize = 12.sp,
                        fontFamily = NotoSansFontFamily,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(24.dp))

                // ── Hàng nút [ HỦY ] và [ ĐỒNG Ý ] ────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Nút Hủy
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF334B63)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextMuted
                        )
                    ) {
                        Text(
                            text = "HỦY",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }

                    // Nút Đồng ý
                    Button(
                        onClick = { submit() },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.str_send_pc_confirm),
                            color = Color.White,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
