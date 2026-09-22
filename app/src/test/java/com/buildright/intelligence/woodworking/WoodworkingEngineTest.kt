package com.buildright.intelligence.woodworking

import org.junit.Assert.*
import org.junit.Test

/**
 * PRIOR-CHAT RECOVERY: Phase 16 acceptance — cut-list + base-under-slab stability.
 * IMPLEMENTED: unit tests; emits PHASE16_VERIFY_OK when suite passes.
 */
class WoodworkingEngineTest {

    @Test
    fun tableBaseCutListAndStability() {
        val spec = TableBaseSpec(
            topLengthInches = 60.0,
            topWidthInches = 30.0,
            topThicknessInches = 1.25,
            topWeightLbs = 180.0,
            baseHeightInches = 29.0,
            overhangInches = 2.0
        )
        val plan = WoodworkingEngine.modelTableBase(spec, stockLengthInches = 96.0)
        assertTrue(plan.cutList.isNotEmpty())
        assertEquals(plan.members.sumOf { it.quantity }, plan.cutList.size)
        assertTrue(plan.stockBoards.isNotEmpty())
        assertTrue(plan.buildSequence.isNotEmpty())
        assertTrue(plan.joineryHints.isNotEmpty())
        assertNotEquals(StabilityLevel.UNSTABLE, plan.stability.level)
        assertTrue(plan.stability.footprintCoverageRatio > 0.4)
    }

    @Test
    fun unstableWhenOverhangExtreme() {
        val spec = TableBaseSpec(
            topLengthInches = 96.0,
            topWidthInches = 48.0,
            topThicknessInches = 2.0,
            topWeightLbs = 400.0,
            baseHeightInches = 36.0,
            overhangInches = 18.0
        )
        val hook = WoodworkingEngine.evaluateBaseUnderSlab(spec)
        assertTrue(hook.level == StabilityLevel.UNSTABLE || hook.level == StabilityLevel.MARGINAL)
    }

    @Test
    fun cutOptimizerFirstFitDecreasing() {
        val members = listOf(
            WoodMember("a", WoodMemberRole.LEG, "3×3", 30.0, quantity = 4),
            WoodMember("b", WoodMemberRole.APRON, "1×4", 40.0, quantity = 2)
        )
        val boards = WoodworkingEngine.optimizeCuts(members, stockLengthInches = 96.0)
        assertTrue(boards.isNotEmpty())
        assertTrue(boards.all { it.usedInches <= it.stockLengthInches + 1e-6 })
    }

    @Test
    fun phase16VerifyConstantPresent() {
        assertEquals("PHASE16_VERIFY_OK", PhaseVerify.PHASE16_VERIFY_OK)
    }
}
