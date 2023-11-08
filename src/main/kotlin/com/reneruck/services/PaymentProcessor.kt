package com.reneruck.services

import com.reneruck.models.CardTransactionRequest
import com.reneruck.models.CreditCard
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.runBlocking
import java.util.UUID

interface PaymentProcessor {
    /**
     * Charge the given card with the amount.
     * The amount is noted in full cents. e.g. 1 €/$/£ = 100
     *
     * Currencies are not considered in this example.
     *
     * @param request credit card number to charge
     */
    fun charge(request: PaymentProcessorRequest): Result<UUID>
}

sealed interface PaymentDetails
data class CardPaymentDetails(val creditCard: CreditCard) : PaymentDetails
data class BankPaymentDetails(val iban: String) : PaymentDetails {
    companion object {
        val dummyCustomerBank = BankPaymentDetails("IE04BOFI900017934739")
    }
}

/**
 * The creditor and debtor payment details are more diverse in real life but will suffice for this example.
 */
data class PaymentProcessorRequest(
    val creditor: PaymentDetails,
    val amount: Int,
    val debtor: PaymentDetails
)

/**
 * The [SimplePaymentProcessorImpl] does not do anything besides some very basic validation.
 *
 * Payment processors are a whole separate world:
 * - Request formats
 * - Outages, Responses and errors
 * - Retries
 * - Currencies
 * - etc
 */
class SimplePaymentProcessorImpl(private val httpClient: HttpClient) : PaymentProcessor {
    companion object {
        private val log = KtorSimpleLogger(this::class.java::getCanonicalName.name)
        private const val PAYMENT_GATEWAY_URL = "https://payment-gateway.com"
    }

    override fun charge(request: PaymentProcessorRequest): Result<UUID> {
        if (request.debtor == request.creditor) return Result.failure(Exception("Debtor and creditor cannot be the same"))
        if (request.amount <= 0) return Result.failure(Exception("Amount must be greater 0"))
        if (exceedsLimit(request)) return Result.failure(Exception("Transaction amount exceeds card limit"))

        log.info("Processing Payment from ${request.debtor} -> ${request.creditor}")

        return runBlocking {
            val response = httpClient.post(PAYMENT_GATEWAY_URL) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(CardTransactionRequest(512, UUID.randomUUID().toString()))
            }

            when (response.status) {
                HttpStatusCode.OK -> Result.success(response.body<PaymentProcessorResponse>().transactionId)
                else -> Result.failure(Exception(response.bodyAsText()))
            }
        }
    }

    /**
     * Card limit is only applied on a per-transaction basis.
     * Bank payment details do not have a limit for now.
     *
     * A card limit of 0 is considered "unlimited".
     */
    private fun exceedsLimit(request: PaymentProcessorRequest): Boolean =
        when (request.debtor) {
            is CardPaymentDetails -> request.debtor.creditCard.limit > 0 && request.amount > request.debtor.creditCard.limit
            is BankPaymentDetails -> false
        }
}

data class PaymentProcessorResponse(val transactionId: UUID)
