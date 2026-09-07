package com.buildright.cost

import org.junit.Assert.*
import org.junit.Test

class PurchasePlannerTest {
    @Test fun roundsUpCompletePackages() {
        val req = MaterialRequirement("nails","Nails",101.0,"pcs")
        val q = PriceQuote("nails","store",null,"sku","Box",100.0,"pcs",10.0)
        val line = PurchasePlanner.line(req,q)
        assertEquals(2, line.packagesToBuy)
        assertEquals(200.0, line.purchasedQuantity, 0.001)
        assertEquals(20.0, line.extendedPrice, 0.001)
    }
}
