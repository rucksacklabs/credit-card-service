package com.reneruck.routes

import com.reneruck.models.CardTransactionRequest
import com.reneruck.models.CreateCardRequest
import com.reneruck.models.CreditCard
import com.reneruck.models.toCreditCard
import com.reneruck.module
import com.reneruck.routes.services.DatabaseContainer
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.mergeWith
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import junit.framework.TestCase.assertEquals
import java.util.UUID
import kotlin.test.Test

class ApplicationTests {

    private val databaseContainer = DatabaseContainer()

    @Test
    fun `POST credit-cards with invalid body should return validation error messages`() = withTestApplication {
        val client = newClient()
        val response = client.post("/credit-cards") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            val req = CreateCardRequest("1234567891112134", null, "11/20", "-15")
            setBody(req)
        }

        val errors = listOf(
            "Name cannot be empty",
            "Unsupported card vendor, supported vendors: VISA, MASTERCARD",
            "Card expired",
            "Invalid limit -15"
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(errors, response.body<List<String>>())
    }

    @Test
    fun `Happy Path POST credit-cards`() = withTestApplication {
        val client = newClient()
        val response = client.post("/credit-cards") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            val req = CreateCardRequest("5234567891112134", "foo", "11/24", "0")
            setBody(req)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET credit-cards should return an empty list`() = withTestApplication {
        val client = newClient()
        val response = client.get("/credit-cards")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(listOf<CreditCard>(), response.body<List<CreditCard>>())
    }

    @Test
    fun `happy path GET credit-cards should return credit cards`() = withTestApplication {
        val client = newClient()
        val card = createCard()
        val response = client.get("/credit-cards")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(listOf(card), response.body<List<CreditCard>>())
    }

    @Test
    fun `GET credit-cards {id} should return 404 when card details not found`() = withTestApplication {
        val cardNumber = "5161864594511286"
        val response = client.get("/credit-cards/$cardNumber")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET credit-cards {id} should return card details`() = withTestApplication {
        val client = newClient()
        val card = createCard()
        val response = client.get("/credit-cards/${card.number}")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(card, response.body<CreditCard>())
    }

    @Test
    fun `put credit-cards {id} should return not found if card does not exist`() = withTestApplication {
        val client = newClient()
        val cardNumber = "5161864594511286"
        val response = client.put("/credit-cards/$cardNumber") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(mapOf("limit" to 1000))
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `put credit-cards {id} with invalid limit should return error`() = withTestApplication {
        val client = newClient()
        val cardNumber = "5161864594511286"
        val response = client.put("/credit-cards/$cardNumber") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(mapOf<String, String>())
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `put credit-cards {id} should update limit`() = withTestApplication {
        val client = newClient()
        val card = createCard()
        val response = client.put("/credit-cards/${card.number}") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(mapOf("limit" to 500))
        }
        assertEquals(HttpStatusCode.OK, response.status)

        val updatedCard = response.body<CreditCard>()
        assertEquals(card.number, updatedCard.number)
        assertEquals(card.name, updatedCard.name)
        assertEquals(card.expiry, updatedCard.expiry)
        assertEquals(500, updatedCard.limit)
    }

    @Test
    fun `happy path delete credit-cards should return Not Found if card not exists`() = withTestApplication {
        val cardNumber = "5161864594511286"
        val response = client.delete("/credit-cards/$cardNumber")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `happy path should delete credit-card`() = withTestApplication {
        val client = newClient()
        val card = createCard()
        val response = client.delete("/credit-cards/${card.number}")
        assertEquals(HttpStatusCode.OK, response.status)

        val getCardResp = client.get("/credit-cards/${card.number}")
        assertEquals(HttpStatusCode.NotFound, getCardResp.status)
    }

    @Test
    fun `happy path post credit-cards {cardNumber} charge`() = withTestApplication {
        val client = newClient()
        val card = createCard()
        val response = client.post("/credit-cards/${card.number}/charge") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(CardTransactionRequest(512, UUID.randomUUID().toString()))
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `happy path post credit-cards {cardNumber} credit`() = withTestApplication {
        val client = newClient()
        val card = createCard()
        val response = client.post("/credit-cards/${card.number}/credit") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(CardTransactionRequest(512, UUID.randomUUID().toString()))
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    private fun <R> withTestApplication(test: suspend ApplicationTestBuilder.() -> R) = testApplication {
        environment { config = overrideConfig(config) }
        application { module() }
        test()
    }

    private fun overrideConfig(config: ApplicationConfig): ApplicationConfig =
        config.mergeWith(
            MapApplicationConfig(
                "db.url" to databaseContainer.postgresContainer.getJdbcUrl(),
                "db.username" to databaseContainer.postgresContainer.username,
                "db.password" to databaseContainer.postgresContainer.password,
                "db.encryptionKey" to "test"
            )
        )

    private fun ApplicationTestBuilder.newClient(): HttpClient = createClient {
        install(ContentNegotiation) { jackson() }
    }

    private suspend fun ApplicationTestBuilder.createCard(): CreditCard {
        val cardRequest = CreateCardRequest(
            number = "5234567891112134",
            name = "foo",
            expiry = "11/24",
            limit = "0"
        )
        val resp = newClient().post("/credit-cards") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(cardRequest)
        }

        assertEquals(HttpStatusCode.OK, resp.status)

        return cardRequest.toCreditCard()
    }
}
