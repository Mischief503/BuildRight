package com.buildright.intelligence.woodworking

import org.junit.Assert.*
import org.junit.Test

/**
 * PRIOR-CHAT RECOVERY: Phase 18 — moisture/movement + joinery gate.
 * IMPLEMENTED: MoistureService tests + PHASE18_VERIFY_OK.
 */
class MoistureServiceTest {

    @Test
    fun tangentialExceedsRadial() {
        val c = MoistureService.coeffsFor("oak")
        assertTrue(c.tangentialPerPercent > c.radialPerPercent)
    }

    @Test
    fun flagsUnsafeCrossGrainAttach() {
        val req = JointRequest(
            id = "m1",
            memberAThicknessIn = 1.5,
            memberBThicknessIn = 0.75,
            loadPath = LoadPath.SHEAR,
            crossGrainRigidAttach = true,
            speciesA = "oak"
        )
        val gate = MoistureService.joineryMovementGate(
            request = req,
            panelWidthInches = 36.0,
            moistureNowPercent = 14.0,
            equilibriumPercent = 7.0,
            useTangential = true
        )
        assertFalse(gate.safe)
        assertTrue(gate.severity == MoistureSeverity.WARN || gate.severity == MoistureSeverity.BLOCK)
        assertTrue(gate.expectedMovementIn > 0.05)
    }

    @Test
    fun joineryEngineIntegratesMoistureBlock() {
        val req = JointRequest(
            id = "m2",
            memberAThicknessIn = 1.5,
            memberBThicknessIn = 1.5,
            tenonThicknessIn = 0.5,
            loadPath = LoadPath.RACKING,
            verified = true,
            crossGrainRigidAttach = true,
            speciesA = "oak"
        )
        val gate = MoistureService.joineryMovementGate(
            request = req,
            panelWidthInches = 40.0,
            moistureNowPercent = 16.0,
            equilibriumPercent = 6.0
        )
        assertEquals(MoistureSeverity.BLOCK, gate.severity)
        val rec = JoineryEngine.recommend(req, gate)
        assertEquals(JointSafety.RED, rec.safety)
        assertTrue(rec.hold)
    }

    @Test
    fun withGrainAttachStaysOk() {
        val req = JointRequest(
            id = "m3",
            memberAThicknessIn = 1.5,
            memberBThicknessIn = 1.5,
            crossGrainRigidAttach = false,
            speciesA = "oak"
        )
        val gate = MoistureService.joineryMovementGate(req, panelWidthInches = 36.0)
        assertTrue(gate.safe)
        assertEquals(MoistureSeverity.OK, gate.severity)
    }

    @Test
    fun phase18VerifyConstantPresent() {
        assertEquals("PHASE18_VERIFY_OK", PhaseVerify.PHASE18_VERIFY_OK)
    }
}
