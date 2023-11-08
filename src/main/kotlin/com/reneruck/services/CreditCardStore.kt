package com.reneruck.services

import com.reneruck.models.CardNumber
import com.reneruck.models.CreditCard
import io.ktor.server.plugins.NotFoundException
import io.ktor.util.logging.KtorSimpleLogger
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.util.UUID

interface CreditCardStore {
    fun create(card: CreditCard): Result<UUID>

    fun list(): Result<List<CreditCard>>
    fun update(card: CreditCard): Result<CreditCard>
    fun get(id: CardNumber): Result<CreditCard?>
    fun delete(id: CardNumber): Result<Unit>
    fun updateLimit(id: CardNumber, newLimit: Int): Result<Boolean>
}

class CreditCardStoreImpl(private val dbConnection: Jdbi, private val encryptionKey: String) : CreditCardStore {

    companion object {
        private val log = KtorSimpleLogger(this::class.java::getCanonicalName.name)
    }

    override fun create(card: CreditCard): Result<UUID> = runCatching {
        dbConnection.withHandle<UUID, Exception> { handle ->
            handle.createUpdate("INSERT INTO \"credit_cards\" (name,number,expiry,card_limit) values (:name, encrypt(:number::bytea, :pwd::bytea, 'aes'::text),:expiry,:limit);")
                .bind("name", card.name)
                .bind("number", card.number)
                .bind("expiry", card.expiry)
                .bind("limit", card.limit.toInt())
                .bind("pwd", encryptionKey)
                .executeAndReturnGeneratedKeys("id")
                .mapTo(UUID::class.java)
                .first()
        }
    }.onFailure {
        log.error("Upsert failed", it)
    }

    override fun update(card: CreditCard): Result<CreditCard> = runCatching {
        dbConnection.withHandle<CreditCard, Exception> { handle ->
            handle.createQuery("UPDATE \"credit_cards\" name,number,expiry,card_limit values (:name, encrypt(:number::bytea, :pwd::bytea, 'aes'::text),:expiry,:limit) where where number = encrypt(:number::bytea, :pwd::bytea, 'aes'::text) returning name, number, expiry, card_limit;")
                .bind("name", card.name)
                .bind("number", card.number)
                .bind("expiry", card.expiry)
                .bind("limit", card.limit.toInt())
                .bind("pwd", encryptionKey)
                .map(::toCreditCard)
                .first()
        }
    }.onFailure {
        log.error("Upsert failed", it)
    }

    override fun get(id: CardNumber): Result<CreditCard?> = runCatching {
        dbConnection.withHandle<CreditCard, Exception> {
            it.select("SELECT name, encode(decrypt(number, :pwd::bytea, 'aes'::text), 'escape') as number, expiry, card_limit from credit_cards where number = encrypt(:number::bytea, :pwd::bytea, 'aes'::text);")
                .bind("pwd", encryptionKey)
                .bind("number", id)
                .map(::toCreditCard)
                .first()
        }
    }.onFailure {
        log.error("Get card $id failed", it)
    }.recover { null }

    override fun delete(id: CardNumber): Result<Unit> = runCatching {
        dbConnection.withHandle<Unit, Exception> {
            val affectedRows = it.createUpdate("delete from credit_cards where number = encrypt(:number::bytea, :pwd::bytea, 'aes'::text);")
                .bind("pwd", encryptionKey)
                .bind("number", id)
                .execute()

            if (affectedRows == 0) throw NotFoundException("No account with number=$id exists")
        }
    }.onFailure {
        log.error("Delete id=$id failed", it)
    }

    override fun list(): Result<List<CreditCard>> = runCatching {
        dbConnection.withHandle<List<CreditCard>, Exception> {
            it.select("SELECT name, encode(decrypt(number, :pwd::bytea, 'aes'::text), 'escape') as number, expiry, card_limit from credit_cards")
                .bind("pwd", encryptionKey)
                .map(::toCreditCard)
                .list()
        }
    }.onFailure {
        log.error("List cards failed", it)
    }

    override fun updateLimit(id: CardNumber, newLimit: Int) = runCatching {
        dbConnection.withHandle<Boolean, Exception> {
            val affectedRows = it.createUpdate("UPDATE \"credit_cards\" set card_limit = :newLimit where number = encrypt(:number::bytea, :pwd::bytea, 'aes'::text); ")
                .bind("pwd", encryptionKey)
                .bind("number", id)
                .bind("newLimit", newLimit.toInt())
                .execute()

            if (affectedRows == 0) throw NotFoundException("No account with number=$id exists")
            affectedRows > 0
        }
    }.onFailure {
        log.error("Update id=$id failed", it)
    }

    private fun toCreditCard(rs: ResultSet, ctx: StatementContext): CreditCard =
        CreditCard(
            rs.getString("number"),
            rs.getString("name"),
            rs.getString("expiry"),
            rs.getInt("card_limit")
        )
}
