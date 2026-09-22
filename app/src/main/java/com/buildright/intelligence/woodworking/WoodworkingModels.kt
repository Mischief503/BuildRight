package com.buildright.intelligence.woodworking

/**
 * PRIOR-CHAT RECOVERY: Phase 16 woodworking geometry (members, panels, grain).
 * IMPLEMENTED: live Kotlin models for furniture / table-base engineering.
 */

enum class GrainAxis { LENGTH, WIDTH, THICKNESS }

enum class WoodMemberRole {
    LEG, APRON, STRETCHER, RAIL, PANEL, TOP_SUPPORT, CROSS_BRACE, FOOT
}

data class WoodMember(
    val id: String,
    val role: WoodMemberRole,
    val nominalSize: String,
    val lengthInches: Double,
    val widthInches: Double = 0.0,
    val thicknessInches: Double = 0.0,
    val grainAxis: GrainAxis = GrainAxis.LENGTH,
    val quantity: Int = 1,
    val note: String = ""
)

data class WoodPanel(
    val id: String,
    val lengthInches: Double,
    val widthInches: Double,
    val thicknessInches: Double,
    val grainAxis: GrainAxis = GrainAxis.LENGTH,
    val species: String = "oak",
    val note: String = ""
)

/**
 * Granite / slab top + wooden base load case recovered from prior-chat ledger.
 */
data class TableBaseSpec(
    val topLengthInches: Double,
    val topWidthInches: Double,
    val topThicknessInches: Double,
    val topWeightLbs: Double,
    val baseHeightInches: Double,
    val legSize: String = "3×3",
    val apronSize: String = "1×4",
    val species: String = "oak",
    val overhangInches: Double = 2.0
)

data class WoodCutRow(
    val number: Int,
    val role: WoodMemberRole,
    val size: String,
    val lengthInches: Double,
    val note: String
)

data class WoodStockBoard(
    val nominalSize: String,
    val stockLengthInches: Double,
    val cuts: MutableList<WoodMember> = mutableListOf()
) {
    val usedInches: Double get() = cuts.sumOf { it.lengthInches * it.quantity }
    val wasteInches: Double get() = stockLengthInches - usedInches
}

enum class StabilityLevel { STABLE, MARGINAL, UNSTABLE }

data class StabilityHook(
    val level: StabilityLevel,
    val tipResistanceFactor: Double,
    val footprintCoverageRatio: Double,
    val message: String
)

data class BuildStep(
    val order: Int,
    val title: String,
    val detail: String
)

data class FurniturePlan(
    val members: List<WoodMember>,
    val panels: List<WoodPanel>,
    val cutList: List<WoodCutRow>,
    val stockBoards: List<WoodStockBoard>,
    val stability: StabilityHook,
    val joineryHints: List<String>,
    val buildSequence: List<BuildStep>,
    val warnings: List<String> = emptyList()
)
