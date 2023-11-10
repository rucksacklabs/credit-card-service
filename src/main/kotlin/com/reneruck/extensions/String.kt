package com.reneruck.extensions

fun String?.sanitizeCardNumber(): String? = this?.trim()?.replace(Regex("[\\s \\-_]"), "")
