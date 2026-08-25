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
        assertEquals("- Thứ tư, ngày 25-08-2026 lúc 17.44: mật khẩu là *** và gọi số ***", masked)
        assertTrue(masked.startsWith("- Thứ tư, ngày 25-08-2026 lúc 17.44:"))
    }
}
