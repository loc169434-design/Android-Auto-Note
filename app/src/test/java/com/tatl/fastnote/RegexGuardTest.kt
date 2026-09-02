package com.tatl.fastnote

import com.tatl.fastnote.util.FileHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegexGuardTest {

    @Test
    fun testMaskPhoneNumbers() {
        val input1 = "Gọi cho tôi số 0912 345 678 vào ngày mai"
        val masked1 = FileHelper.maskContent(input1)
        assertEquals("Gọi cho tôi số *** vào ngày mai", masked1)

        val input2 = "SĐT: 0912345678"
        val masked2 = FileHelper.maskContent(input2)
        assertEquals("SĐT: ***", masked2)

        val input3 = "CMND cũ: 123 456 789"
        val masked3 = FileHelper.maskContent(input3)
        assertEquals("CMND cũ: ***", masked3)
    }

    @Test
    fun testMaskBankCardAndAccount() {
        val input1 = "Số thẻ Visa là 1234 5678 9012 3456 để thanh toán"
        val masked1 = FileHelper.maskContent(input1)
        assertEquals("Số thẻ Visa là *** để thanh toán", masked1)

        val input2 = "STK MB: 12345678901234"
        val masked2 = FileHelper.maskContent(input2)
        assertEquals("STK MB: ***", masked2)
    }

    @Test
    fun testMaskPasswordVariants() {
        assertEquals("mk: ***", FileHelper.maskContent("mk: 123456"))
        assertEquals("pass là ***", FileHelper.maskContent("pass là Abc@123"))
        assertEquals("mật khẩu-***", FileHelper.maskContent("mật khẩu-xyz"))
        assertEquals("password = ***", FileHelper.maskContent("password = mySecretPass"))
    }

    @Test
    fun testMaskOtpAndPin() {
        assertEquals("mã pin: ***", FileHelper.maskContent("mã pin: 1234"))
        assertEquals("otp là ***", FileHelper.maskContent("otp là 589201"))
        assertEquals("mã xác nhận: ***", FileHelper.maskContent("mã xác nhận: 9988"))
    }

    @Test
    fun testMaskApiKeysAndTokens() {
        val apiKey = "AIzaSyD-12345aBcdEFGHiJKLmn"
        val input = "Google API Key: $apiKey"
        val masked = FileHelper.maskContent(input)
        assertEquals("Google API Key: ***", masked)
    }

    @Test
    fun testMaskEmail() {
        val input = "Liên hệ qua email user.name@domain.com nhé"
        val masked = FileHelper.maskContent(input)
        assertEquals("Liên hệ qua email *** nhé", masked)
    }

    @Test
    fun testMaskLinePreservesDateHeader() {
        val line = "- Thứ tư, ngày 25-08-2026 lúc 17.44: mật khẩu là 123456 và gọi số 0912 345 678"
        val masked = FileHelper.maskSensitive(listOf(line)).first()
        assertTrue(masked.startsWith("- Thứ tư, ngày 25-08-2026 lúc 17.44:"))
    }

    @Test
    fun testFilterForAiSharingDeletesSensitiveKeywordLines() {
        val inputLines = listOf(
            "- Thứ Hai, 31-08-2026 08:00: Hôm nay ăn cơm với cá",
            "- Thứ Hai, 31-08-2026 09:00: mk tài khoản nhà phát triển là 123456",
            "- Thứ Hai, 31-08-2026 10:00: 1357999 là pass gmail",
            "- Thứ Hai, 31-08-2026 11:00: mã pin thẻ atm là 9988",
            "- Thứ Hai, 31-08-2026 12:00: Mã OTP xác thực là 456789",
            "- Thứ Hai, 31-08-2026 13:00: my password is SuperSecret123",
            "- Thứ Hai, 31-08-2026 14:00: パスワードは 123456 です",
            "- Thứ Hai, 31-08-2026 15:00: Mein Passwort ist geheim",
            "- Thứ Hai, 31-08-2026 16:00: Мой пароль от почты 778899",
            "- Thứ Hai, 31-08-2026 17:00: Đi siêu thị mua sữa cho con và gọi số 0912345678"
        )

        val filtered = FileHelper.filterForAiSharing(inputLines)

        // Chỉ còn lại 2 dòng không chứa từ khóa nhạy cảm
        assertEquals(2, filtered.size)
        assertEquals("- Thứ Hai, 31-08-2026 08:00: Hôm nay ăn cơm với cá", filtered[0])
        assertEquals("- Thứ Hai, 31-08-2026 17:00: Đi siêu thị mua sữa cho con và gọi số ***", filtered[1])
    }

    @Test
    fun testFilterNumbers9_10_12_16() {
        val inputLines = listOf(
            "CMND 9 số: 123456789",
            "SĐT 10 số: 0987654321",
            "CCCD 12 số: 001234567890",
            "Thẻ 16 số: 4123 4567 8901 2345"
        )

        val filtered = FileHelper.filterForAiSharing(inputLines)
        assertEquals(4, filtered.size)
        assertEquals("CMND 9 số: ***", filtered[0])
        assertEquals("SĐT 10 số: ***", filtered[1])
        assertEquals("CCCD 12 số: ***", filtered[2])
        assertEquals("Thẻ 16 số: ***", filtered[3])
    }

    @Test
    fun testFilterBypassLayer1() {
        val inputLines = listOf(
            "CMND 9 số: 123456789",
            "SĐT 10 số: 0987654321",
            "Gặp anh Nam lúc 3h"
        )

        // Khi bypassLayer1 = true -> Số 9, 10 chữ số KHÔNG bị che thành ***
        val filtered = FileHelper.filterForAiSharing(inputLines, bypassLayer1 = true)
        assertEquals(3, filtered.size)
        assertEquals("CMND 9 số: 123456789", filtered[0])
        assertEquals("SĐT 10 số: 0987654321", filtered[1])
        assertEquals("Gặp anh Nam lúc 3h", filtered[2])
    }
}
