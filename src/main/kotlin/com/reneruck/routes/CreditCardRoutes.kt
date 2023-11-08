package com.reneruck.routes

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.reneruck.models.CardTransactionRequest
import com.reneruck.models.CreateCardRequest
import com.reneruck.models.toCreditCard
import com.reneruck.services.CreditCardService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondNullable
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.util.getOrFail

fun Routing.creditCardRoutes(creditCardService: CreditCardService) {
    post<CreateCardRequest>("/credit-cards") { req ->
        creditCardService.addCard(req.toCreditCard()).toResult(this.call)
    }

    get("/credit-cards") {
        creditCardService.listCards().toResult(this.call)
    }

    get("/credit-cards/{cardNumber}") {
        val cardNumber = call.parameters.getOrFail("cardNumber")
        creditCardService.getCard(cardNumber).toResult(this.call)
    }

    put("/credit-cards/{cardNumber}") {
        val cardNumber = call.parameters.getOrFail("cardNumber")
        val requestBody = call.receiveText()

        val newLimit = runCatching { jacksonObjectMapper().readTree(requestBody).get("limit").asText() }
            .map { it.toInt() }
            .getOrNull()

        if (newLimit == null) {
            call.respondNullable(HttpStatusCode.BadRequest, "Invalid limit $requestBody")
        } else {
            creditCardService.updateLimit(cardNumber, newLimit).toResult(this.call)
        }
    }

    delete("/credit-cards/{cardNumber}") {
        val cardNumber = call.parameters.getOrFail("cardNumber")
        creditCardService.deleteCard(cardNumber).toResult(call)
    }

    post("/credit-cards/{cardNumber}/charge") {
        val request = call.receive<CardTransactionRequest>()
        val cardNumber = call.parameters.getOrFail("cardNumber")

        creditCardService.chargeCard(cardNumber, request)
            .map { mapOf("transaction_id" to it) }
            .toResult(call)
    }

    post("/credit-cards/{cardNumber}/credit") {
        val request = call.receive<CardTransactionRequest>()
        val cardNumber = call.parameters.getOrFail("cardNumber")

        creditCardService.creditCard(cardNumber, request)
            .map { mapOf("transaction_id" to it) }
            .toResult(call)
    }
}

suspend inline fun <reified T> Result<T>.toResult(call: ApplicationCall) {
    fold(
        {
            val statusCode = it?.let { HttpStatusCode.OK } ?: HttpStatusCode.NotFound
            call.respondNullable(statusCode, it)
        },
        {
            when (it) {
                is NotFoundException -> call.respond(HttpStatusCode.NotFound, it.message ?: "An internal error happened")
                else -> call.respond(HttpStatusCode.InternalServerError, it.message ?: "An internal error happened")
            }
        }
    )
}
