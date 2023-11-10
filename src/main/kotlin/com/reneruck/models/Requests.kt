package com.reneruck.models

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Resource("/credit-cards")
data class CreateCardRequest(val number: String? = null, val name: String? = null, val expiry: String? = null, val limit: String? = null)

/**
 * Only use after applying validation on [CreateCardRequest]!
 */
fun CreateCardRequest.toCreditCard() =
    CreditCard(
        number?.trim()?.replace(Regex("[\\w\\-_]"), "")!!,
        name!!,
        expiry!!,
        limit?.toInt()!!
    )

@Serializable
data class CardTransactionRequest(val amount: Int = 0, val shopId: String?)
