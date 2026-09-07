package com.buildright.cost

object PriceFreshness {
    const val DEFAULT_MAX_AGE_MS: Long = 24L * 60L * 60L * 1000L

    fun isStale(
        quote: PriceQuote,
        now: Long = System.currentTimeMillis(),
        maxAgeMs: Long = DEFAULT_MAX_AGE_MS
    ): Boolean = now - quote.checkedAt > maxAgeMs

    fun ageHours(quote: PriceQuote, now: Long = System.currentTimeMillis()): Double =
        (now - quote.checkedAt).coerceAtLeast(0) / 3_600_000.0
}
