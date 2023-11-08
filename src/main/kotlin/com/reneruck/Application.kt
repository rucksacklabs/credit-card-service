package com.reneruck

import com.fasterxml.jackson.databind.SerializationFeature
import com.reneruck.models.cardCreationRequestValidation
import com.reneruck.models.cardTransactionRequestValidation
import com.reneruck.routes.creditCardRoutes
import com.reneruck.services.CreditCardServiceImpl
import com.reneruck.services.CreditCardStore
import com.reneruck.services.CreditCardStoreImpl
import com.reneruck.services.SimplePaymentProcessorImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.resources.Resources
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.flywaydb.core.Flyway
import org.jdbi.v3.core.Jdbi
import org.slf4j.event.Level
import java.util.*
import kotlin.random.Random

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val config = environment.config
    val dbConfig = config.getDbConfig()

    configureCallLogging()
    configureMetrics()
    configureContentNegotiation()
    configureRequestValidation()
    configureErrorHandling()

    migrateDatabase(dbConfig)

    // TODO if this gets any more elaborate, introduce dependency injection
    val cardStore = buildCreditcardStore(dbConfig)
    val paymentProcessor = SimplePaymentProcessorImpl(mockHttpClient(config))
    val creditCardService = CreditCardServiceImpl(cardStore, paymentProcessor)

    install(Resources)

    install(Routing) {
        creditCardRoutes(creditCardService)
    }
}

private fun Application.configureRequestValidation() {
    install(RequestValidation) {
        cardTransactionRequestValidation()
        cardCreationRequestValidation()
    }
}

private fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.reasons)
        }
        exception<BadRequestException> { call, _ ->
            call.respondText(text = "Invalid request", status = HttpStatusCode.BadRequest)
        }
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
}

fun migrateDatabase(dbConfig: DbConfig) {
    Flyway.configure().dataSource(dbConfig.url, dbConfig.username, dbConfig.password).load().migrate()
}

fun buildCreditcardStore(dbConfig: DbConfig): CreditCardStore {
    val jdbi = Jdbi.create(dbConfig.url, dbConfig.username, dbConfig.password)

    return CreditCardStoreImpl(jdbi, dbConfig.encryptionKey)
}

fun Application.configureCallLogging() {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
        callIdMdc("call-id")
    }
    install(CallId) {
        header(HttpHeaders.XRequestId)
        verify { callId: String ->
            callId.isNotEmpty()
        }
    }
}

fun Application.configureMetrics() {
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
    }
}

fun Application.configureContentNegotiation() {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
}

fun mockHttpClient(config: ApplicationConfig): HttpClient =
    HttpClient(MockEngine) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) { jackson { } }
        engine {
            addHandler { request ->
                if (request.url.host == "payment-gateway.com") {
                    // configurable fail rate 0.0 = 0% failure 1.0 = 100% failures
                    val failRate = config.config("payment_gateway").propertyOrNull("fail_rate")?.getString()?.toDouble() ?: 0.0

                    if (Random.nextDouble(0.0, 1.0) < failRate) {
                        respond("Something went wrong", HttpStatusCode.InternalServerError)
                    } else {
                        respond("""{"transactionId": "${UUID.randomUUID()}"}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
                    }
                } else {
                    error("Unhandled ${request.url}")
                }
            }
        }
    }

fun ApplicationConfig.getDbConfig(): DbConfig =
    DbConfig(
        url = property("db.url").getString(),
        username = property("db.username").getString(),
        password = property("db.password").getString(),
        encryptionKey = property("db.encryptionKey").getString()
    )

data class DbConfig(val url: String, val username: String, val password: String, val encryptionKey: String)
