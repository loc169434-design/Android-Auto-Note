package com.tatl.fastnote.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tatl.fastnote.R

private val BgDialog   = Color(0xFF1A1A1A)
private val BgField    = Color(0xFF000000)
private val BgButton   = Color(0xFF2A2A2A)
private val TextWhite  = Color(0xFFFFFFFF)
private val TextGray   = Color(0xFF888888)
private val TextError  = Color(0xFFFF5252)
private val Divider    = Color(0xFF333333)

/**
 * Popup "Gửi PC" — nhập mật khẩu để nén AES-256.
 *
 * Quy tắc mật khẩu:
 *  - Chỉ chấp nhận chữ [a-zA-Z] và số [0-9] (không dấu, không ký tự đặc biệt)
 *  - Tối thiểu 6 ký tự
 */
@Composable
fun SendPcDialog(
    onDismiss: () -> Unit,
    onConfirm: (password: String) -> Unit
) {
    var password  by remember { mutableStateOf("") }
    var errorMsg  by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .background(BgDialog, shape = RoundedCornerShape(16.dp))
                .padding(top = 28.dp, start = 24.dp, end = 24.dp, bottom = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Tiêu đề ───────────────────────────────────────────────────────
            Text(
                text = stringResource(R.string.str_send_pc_title),
                color = TextWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )

            Spacer(Modifier.height(20.dp))

            // ── Ô nhập mật khẩu ───────────────────────────────────────────────
            TextField(
                value = password,
                onValueChange = { raw ->
                    // Chỉ chấp nhận a-z A-Z 0-9
                    val filtered = raw.filter { it.isLetterOrDigit() && it.code < 128 }
                    password = filtered
                    errorMsg = ""
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedTextColor        = TextWhite,
                    unfocusedTextColor      = TextWhite,
                    focusedContainerColor   = BgField,
                    unfocusedContainerColor = BgField,
                    focusedIndicatorColor   = Divider,
                    unfocusedIndicatorColor = Divider,
                    cursorColor             = TextWhite
                ),
                placeholder = {
                    Text("••••••••", color = TextGray, fontSize = 20.sp)
                }
            )

            // ── Lỗi validation ────────────────────────────────────────────────
            if (errorMsg.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = errorMsg,
                    color = TextError,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Đường kẻ ngang ────────────────────────────────────────────────
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
            ) {
                drawRect(color = Divider)
            }

            // ── Nút ĐỒNG Ý ───────────────────────────────────────────────────
            Button(
                onClick = {
                    when {
                        password.length < 6 ->
                            errorMsg = "Mật khẩu tối thiểu 6 ký tự"
                        !password.all { it.isLetterOrDigit() && it.code < 128 } ->
                            errorMsg = "Chỉ dùng chữ và số không dấu (a-z, A-Z, 0-9)"
                        else -> onConfirm(password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 0.dp, topEnd = 0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BgButton),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text(
                    text = stringResource(R.string.str_send_pc_confirm),
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
