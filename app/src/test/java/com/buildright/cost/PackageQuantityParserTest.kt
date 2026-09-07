package com.buildright.cost

import org.junit.Assert.assertEquals
import org.junit.Test

class PackageQuantityParserTest {
    @Test fun parsesPackAndAreaDescriptions() {
        val pack = PackageQuantityParser.fromText("Premium framing studs 10-pack")
        assertEquals(10.0, pack.first, 0.001)
        assertEquals("pcs", pack.second)

        val area = PackageQuantityParser.fromText("Roof underlayment 400 sq ft roll")
        assertEquals(400.0, area.first, 0.001)
        assertEquals("sq ft", area.second)
    }
}
