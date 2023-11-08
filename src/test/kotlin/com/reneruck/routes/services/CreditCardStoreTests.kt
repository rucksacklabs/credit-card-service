package com.reneruck.routes.services

import com.reneruck.models.CreditCard
import com.reneruck.services.CreditCardStoreImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class CreditCardStoreTests {

    private val databaseContainer = DatabaseContainer()

    private val creditCardStore = CreditCardStoreImpl(databaseContainer.dbConnection, "secret")

    @Test
    fun `list on empty table should result empty list`() {
        assertSuccess(emptyList(), creditCardStore.list())
    }

    @Test
    fun `insert should return entry id`() {
        val card = CreditCard(
            "123456",
            "foobar",
            "11/23",
            0
        )
        val res = creditCardStore.create(card)

        assertTrue(res.isSuccess)
    }

    @Test
    fun `get should return entry is exists`() {
        val card = CreditCard(
            "1234567",
            "foobar",
            "11/23",
            0
        )
        val res = creditCardStore.create(card)

        assertTrue(res.isSuccess)

        val res2 = creditCardStore.get(card.number)
        assertSuccess(card, res2)
    }

    @Test
    fun `list should return all entries in the DB`() {
        val card = CreditCard(
            "12345678",
            "foobar",
            "11/23",
            0
        )
        val res = creditCardStore.create(card)
        assertTrue(res.isSuccess)

        val entries = creditCardStore.list()

        assertTrue(entries.isSuccess)
        assertSuccess(listOf(card), entries)
    }

    @Test
    fun `update limit should return updated entry`() {
        val card = CreditCard(
            "1234567",
            "foobar",
            "11/23",
            150
        )
        assertTrue(creditCardStore.create(card).isSuccess)
        assertSuccess(true, creditCardStore.updateLimit(card.number, 500))

        val updatedCard = creditCardStore.get(card.number).getOrThrow()

        assertEquals(card.number, updatedCard?.number)
        assertEquals(card.name, updatedCard?.name)
        assertEquals(card.expiry, updatedCard?.expiry)
        assertEquals(500, updatedCard?.limit)
    }
}

fun <T> assertSuccess(expected: T, res: Result<T>) {
    if (res.isFailure) fail("Expected success but was failure")
    assertEquals(expected, res.getOrThrow())
}
