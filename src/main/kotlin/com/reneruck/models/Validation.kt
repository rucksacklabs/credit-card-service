package com.reneruck.models

import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.plugins.requestvalidation.ValidationResult
import io.ktor.server.plugins.requestvalidation.ValidationResult.Invalid
import io.ktor.server.plugins.requestvalidation.ValidationResult.Valid
import java.time.Month
import java.time.YearMonth
import java.util.UUID

fun RequestValidationConfig.cardCreationRequestValidation() {
    isValidName()
    isValidCardNumber()
    isSupportedVendor()
    isNotExpired()
    isValidLimit()
}

fun RequestValidationConfig.cardTransactionRequestValidation() {
    validate<CardTransactionRequest> {
        if (it.amount <= 0) {
            Invalid("Transaction amount must be greater 0")
        } else {
            Valid
        }
    }
    validate<CardTransactionRequest> {
        if (runCatching { UUID.fromString(it.shopId) }.getOrNull() == null) {
            Invalid("Invalid shop ID")
        } else {
            Valid
        }
    }
}

fun RequestValidationConfig.isNotExpired() = validate<CreateCardRequest> { isNotExpired(it.expiry) }
fun RequestValidationConfig.isValidLimit() = validate<CreateCardRequest> { isValidLimit(it.limit) }
fun RequestValidationConfig.isValidCardNumber() = validate<CreateCardRequest> { isValidCardNumber(it.number) }
fun RequestValidationConfig.isSupportedVendor() = validate<CreateCardRequest> { isSupportedVendor(it.number) }
fun RequestValidationConfig.isValidName() = validate<CreateCardRequest> { isValidName(it.name) }

fun isNotExpired(expiry: String?): ValidationResult {
    if (expiry == null) return Invalid("Expiry missing")

    return Regex("(0[1-9]|1[0-2])/([2-9][3-9])").matchEntire(expiry)
        ?.destructured
        ?.let { YearMonth.of("20${it.component2()}".toInt(), Month.of(it.component1().toInt())) }
        ?.isBefore(YearMonth.now())
        ?.let { Valid }
        ?: Invalid("Card expired")
}

fun isValidLimit(limit: String?): ValidationResult =
    runCatching {
        limit?.toIntOrNull()?.takeIf { it >= 0 }!!
    }.fold({ Valid }, { Invalid("Invalid limit $limit") })

fun isValidCardNumber(number: String?): ValidationResult =
    if (number == null || !number.matches(Regex("[0-9]{15,16}"))) {
        Invalid("Invalid card number. Must be 15 or 16 digits long")
    } else {
        Valid
    }

fun isSupportedVendor(number: String?): ValidationResult {
    val firstNumber = runCatching { number?.first()?.digitToInt() }.getOrNull()
    return if (AcceptedVendors.entries.any { firstNumber == it.idDigit }) {
        Valid
    } else {
        Invalid("Unsupported card vendor, supported vendors: ${AcceptedVendors.entries.joinToString(", ")}")
    }
}

fun isValidName(name: String?): ValidationResult =
    if (name.isNullOrBlank()) {
        Invalid("Name cannot be empty")
    } else {
        Valid
    }

enum class AcceptedVendors(val idDigit: Int) {
    VISA(4),
    MASTERCARD(5)
}
