package com.reneruck.models

typealias CardNumber = String

data class CreditCard(
    val number: CardNumber,
    val name: String,
    val expiry: String,
    val limit: Int
)
