package com.reneruck.services

import com.reneruck.models.CardNumber
import com.reneruck.models.CardTransactionRequest
import com.reneruck.models.CreditCard
import com.reneruck.services.BankPaymentDetails.Companion.dummyCustomerBank
import io.ktor.server.plugins.NotFoundException
import java.lang.IllegalArgumentException
import java.util.UUID

interface CreditCardService {
    /**
     * Register a new [CreditCard] with the system.
     *
     * @return UUID the card details unique identifier
     */
    fun addCard(card: CreditCard): Result<UUID>

    /**
     * Get the card details for a given card number
     *
     * @return [CreditCard] details or null if not exits
     */
    fun getCard(cardNumber: CardNumber): Result<CreditCard?>

    /**
     *
     */
    fun updateLimit(cardNumber: CardNumber, limit: Int): Result<CreditCard?>
    fun deleteCard(cardNumber: CardNumber): Result<Unit>
    fun listCards(): Result<List<CreditCard>>

    fun chargeCard(customerCardNumber: CardNumber, cardTransactionRequest: CardTransactionRequest): Result<UUID>
    fun creditCard(customerCardNumber: CardNumber, cardTransactionRequest: CardTransactionRequest): Result<UUID>
}

class CreditCardServiceImpl(private val cardStore: CreditCardStore, private val cardProcessor: PaymentProcessor) :
    CreditCardService {
    override fun addCard(card: CreditCard): Result<UUID> {
        return cardStore.create(card)
    }

    override fun getCard(cardNumber: CardNumber): Result<CreditCard?> {
        return cardStore.get(cardNumber)
    }

    override fun updateLimit(cardNumber: CardNumber, limit: Int): Result<CreditCard?> {
        return cardStore.updateLimit(cardNumber, limit)
            .fold(
                { if (!it) Result.success(null) else cardStore.get(cardNumber) },
                { Result.failure(it) }
            )
    }

    override fun deleteCard(cardNumber: CardNumber): Result<Unit> {
        return cardStore.delete(cardNumber)
    }

    override fun listCards(): Result<List<CreditCard>> {
        return cardStore.list()
    }

    override fun chargeCard(customer: CardNumber, cardTransactionRequest: CardTransactionRequest): Result<UUID> {
        val customerPaymentDetails = shopPaymentDetails(cardTransactionRequest.shopId!!).getOrThrow()

        val toPaymentProcessorRequest: (CreditCard?) -> PaymentProcessorRequest = { card ->
            if (card == null) throw IllegalArgumentException("Unknown card details for number $customer")
            PaymentProcessorRequest(
                debtor = CardPaymentDetails(card),
                amount = cardTransactionRequest.amount,
                creditor = customerPaymentDetails
            )
        }

        return cardStore.get(customer)
            .mapCatching(toPaymentProcessorRequest)
            .fold(
                { cardProcessor.charge(it) },
                { Result.failure(it) }
            )
    }

    override fun creditCard(customerCardNumber: CardNumber, cardTransactionRequest: CardTransactionRequest): Result<UUID> {
        val customerPaymentDetails = shopPaymentDetails(cardTransactionRequest.shopId!!).getOrThrow()

        val toPaymentProcessorRequest: (CreditCard?) -> PaymentProcessorRequest = { card ->
            if (card == null) throw IllegalArgumentException("Unknown card details for number $customerCardNumber")
            PaymentProcessorRequest(
                debtor = customerPaymentDetails,
                amount = cardTransactionRequest.amount,
                creditor = CardPaymentDetails(card)
            )
        }

        val cardDetails = cardStore.get(customerCardNumber)

        return cardDetails
            .fold(
                { it?.let { Result.success(it) } ?: Result.failure(NotFoundException("No card details found")) },
                { Result.failure(it) }
            )
            .mapCatching(toPaymentProcessorRequest)
            .fold(
                { cardProcessor.charge(it) },
                { Result.failure(it) }
            )
    }

    /**
     * This is a dummy implementation
     * Somewhere we need to keep track of shops and their banking details in order to facilitate payments.
     */
    private fun shopPaymentDetails(shopId: String): Result<PaymentDetails> = Result.success(dummyCustomerBank)
}
