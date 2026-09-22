package com.buildright.intelligence.woodworking

import org.junit.Assert.*
import org.junit.Test

/**
 * PRIOR-CHAT RECOVERY: Phase 17 — AUTO=MORTISE_TENON; YELLOW/GREEN; impossible RED HOLD.
 * IMPLEMENTED: JoineryEngine tests + PHASE17_VERIFY_OK.
 */
class JoineryEngineTest {

    @Test
    fun autoRecommendsMortiseTenon() {
        val req = JointRequest(
            id = "j1",
            memberAThicknessIn = 1.5,
            memberBThicknessIn = 1.5,
            tenonThicknessIn = 0.5,
            tenonLengthIn = 1.25,
            loadPath = LoadPath.RACKING
        )
        val rec = JoineryEngine.recommend(req)
        assertEquals(JointType.MORTISE_TENON, rec.recommended)
        assertEquals("AUTO=MORTISE_TENON", rec.autoMarker)
        assertEquals(JointSafety.YELLOW, rec.safety)
        assertFalse(rec.hold)
    }

    @Test
    fun verifiedIsGreen() {
        val req = JointRequest(
            id = "j2",
            memberAThicknessIn = 1.5,
            memberBThicknessIn = 1.5,
            tenonThicknessIn = 0.5,
            tenonLengthIn = 1.0,
            loadPath = LoadPath.RACKING,
            verified = true
        )
        val rec = JoineryEngine.recommend(req)
        assertEquals(JointSafety.GREEN, rec.safety)
        assertEquals("AUTO=MORTISE_TENON", rec.autoMarker)
    }

    @Test
    fun impossibleTenonIsRedHold() {
        val req = JointRequest(
            id = "j3",
            memberAThicknessIn = 1.5,
            memberBThicknessIn = 1.5,
            tenonThicknessIn = 1.25, // > 1/3 of 1.5
            tenonLengthIn = 2.0,
            loadPath = LoadPath.RACKING,
            preferred = JointType.MORTISE_TENON
        )
        assertTrue(JoineryEngine.isImpossibleTenon(req))
        val rec = JoineryEngine.recommend(req)
        assertEquals(JointSafety.RED, rec.safety)
        assertTrue(rec.hold)
        assertTrue(rec.reasons.any { it.contains("Impossible tenon", ignoreCase = true) })
    }

    @Test
    fun toRecordPersistsChoice() {
        val req = JointRequest(
            id = "persist-1",
            memberAThicknessIn = 1.5,
            memberBThicknessIn = 1.5,
            tenonThicknessIn = 0.5,
            loadPath = LoadPath.RACKING,
            verified = true
        )
        val rec = JoineryEngine.recommend(req)
        val record = JoineryEngine.toRecord(rec, "leg", "apron")
        assertEquals("MORTISE_TENON", record.jointType)
        assertEquals("GREEN", record.safety)
        assertEquals("AUTO=MORTISE_TENON", record.autoMarker)
    }

    @Test
    fun phase17VerifyConstantPresent() {
        assertEquals("PHASE17_VERIFY_OK", PhaseVerify.PHASE17_VERIFY_OK)
    }
}
