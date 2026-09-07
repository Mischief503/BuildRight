package com.buildright.app

import org.junit.Assert.*
import org.junit.Test

class BuildEngineTest {
    @Test fun shedProducesTakeoff() {
        val p = Project(name="Test Shed", type="Shed", width=12.0, length=16.0, height=8.0)
        val r = BuildEngine.calculateShed(p)
        assertEquals(192.0, r.floorAreaSqFt, 0.001)
        assertTrue(r.wallStudCount > 0)
        assertTrue(r.floorJoistCount > 0)
        assertTrue(r.lines.isNotEmpty())
        assertTrue(r.cutList.isNotEmpty())
    }

    @Test fun invalidDimensionsReturnWarning() {
        val p = Project(type="Shed", width=0.0, length=16.0, height=8.0)
        val r = BuildEngine.calculateShed(p)
        assertEquals(0.0, r.floorAreaSqFt, 0.0)
        assertTrue(r.warnings.isNotEmpty())
    }
}
