package com.buildright.cost

import org.junit.Assert.assertEquals
import org.junit.Test

class PriceComparisonEngineTest {
    @Test fun splitCartCanBeatEverySingleStoreAfterPackageRounding() {
        val req = listOf(
            MaterialRequirement("stud", "2x4x8 studs", 43.0, "pcs"),
            MaterialRequirement("osb", "OSB sheets", 12.0, "sheets")
        )
        val quotes = listOf(
            PriceQuote("stud", "A", null, "a1", "Stud 10-pack", 10.0, "pcs", 30.0),
            PriceQuote("stud", "B", null, "b1", "Stud single", 1.0, "pcs", 3.75),
            PriceQuote("osb", "A", null, "a2", "OSB", 1.0, "sheets", 18.0),
            PriceQuote("osb", "B", null, "b2", "OSB", 1.0, "sheets", 17.0)
        )
        val result = PriceComparisonEngine.compare(req, quotes)
        assertEquals(354.0, result.cheapestSplit.total, 0.001)
        assertEquals(2, result.cheapestSplit.retailerCount)
        assertEquals("B", result.cheapestCompleteSingleStore?.retailerId)
        assertEquals(365.25, result.cheapestCompleteSingleStore?.plan?.total ?: 0.0, 0.001)
        assertEquals(11.25, result.splitSavingsVsSingleStore, 0.001)
    }
}
