package com.reneruck.routes.models

import com.reneruck.models.isNotExpired
import com.reneruck.models.isSupportedVendor
import com.reneruck.models.isValidCardNumber
import com.reneruck.models.isValidLimit
import com.reneruck.models.isValidName
import io.ktor.server.plugins.requestvalidation.ValidationResult.Invalid
import io.ktor.server.plugins.requestvalidation.ValidationResult.Valid
import kotlin.test.Test
import kotlin.test.assertTrue

class ValidationTests {
    @Test
    fun `validate CreateCreditCardRequest`() {
    }

    @Test
    fun `validate isValidName`() {
        assertTrue(isValidName("") is Invalid)
        assertTrue(isValidName(null) is Invalid)
        assertTrue(isValidName("      ") is Invalid)
        assertTrue(isValidName("foobar") is Valid)
    }

    @Test
    fun `validate isValidCardNumber`() {
        assertTrue(isValidCardNumber(null) is Invalid)
        assertTrue(isValidCardNumber("123456") is Invalid)
        assertTrue(isValidCardNumber("") is Invalid)
        assertTrue(isValidCardNumber("123456789123456") is Valid)
        assertTrue(isValidCardNumber("1234567891234567") is Valid)
    }

    @Test
    fun `validate isSupportedVendor`() {
        assertTrue(isSupportedVendor(null) is Invalid)
        assertTrue(isSupportedVendor("") is Invalid)
        assertTrue(isSupportedVendor("123456789123456") is Invalid)
        assertTrue(isSupportedVendor("1234567891234567") is Invalid)
        assertTrue(isSupportedVendor("512345678912345") is Valid)
        assertTrue(isSupportedVendor("412345678912345") is Valid)
        assertTrue(isSupportedVendor("5123456789123456") is Valid)
        assertTrue(isSupportedVendor("4123456789123456") is Valid)
    }

    @Test
    fun `validate isNotExpired`() {
        assertTrue(isNotExpired(null) is Invalid)
        assertTrue(isNotExpired("") is Invalid)
        assertTrue(isNotExpired("15/99") is Invalid)
        assertTrue(isNotExpired("narjghekjgf") is Invalid)
        assertTrue(isNotExpired("12/2022") is Invalid)
        assertTrue(isNotExpired("0522") is Invalid)
        assertTrue(isNotExpired("09/20") is Invalid)
        assertTrue(isNotExpired("22/22") is Invalid)
        assertTrue(isNotExpired("01/25") is Valid)
    }

    @Test
    fun `validate isValidLimit`() {
        assertTrue(isValidLimit(null) is Invalid)
        assertTrue(isValidLimit("") is Invalid)
        assertTrue(isValidLimit("-15") is Invalid)
        assertTrue(isValidLimit("123") is Valid)
        assertTrue(isValidLimit("0") is Valid)
    }
}
