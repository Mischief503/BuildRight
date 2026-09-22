package com.buildright.intelligence.woodworking

import kotlin.math.abs

/**
 * PRIOR-CHAT RECOVERY: Phase 18 Wood Moisture & Movement (v2.8.0 / VC37).
 * IMPLEMENTED: species/grain, radial vs tangential, acclimation, movement-safe
 * attachments, breadboard guidance; gates Phase 17 joinery.
 */

enum class MoistureSeverity { OK, WARN, BLOCK }

data class MoistureGateResult(
    val safe: Boolean,
    val severity: MoistureSeverity,
    val message: String,
    val expectedMovementIn: Double = 0.0
)

data class SpeciesMovementCoeffs(
    val species: String,
    /** Fractional change tangential per 1% MC change (approx). */
    val tangentialPerPercent: Double,
    /** Fractional change radial per 1% MC change (approx). */
    val radialPerPercent: Double
)

object MoistureService {

    private val catalog = mapOf(
        "oak" to SpeciesMovementCoeffs("oak", 0.0036, 0.0018),
        "maple" to SpeciesMovementCoeffs("maple", 0.0032, 0.0016),
        "walnut" to SpeciesMovementCoeffs("walnut", 0.0028, 0.0015),
        "pine" to SpeciesMovementCoeffs("pine", 0.0025, 0.0012),
        "cherry" to SpeciesMovementCoeffs("cherry", 0.0026, 0.0013)
    )

    fun coeffsFor(species: String): SpeciesMovementCoeffs =
        catalog[species.lowercase()] ?: SpeciesMovementCoeffs(species.lowercase(), 0.0030, 0.0015)

    /**
     * Expected dimensional change across [widthInches] for MC delta along grain plane.
     */
    fun expectedMovementInches(
        species: String,
        widthInches: Double,
        moistureNowPercent: Double,
        equilibriumPercent: Double,
        grain: GrainAxis = GrainAxis.WIDTH,
        useTangential: Boolean = true
    ): Double {
        val c = coeffsFor(species)
        val rate = if (useTangential) c.tangentialPerPercent else c.radialPerPercent
        // WIDTH/THICKNESS move across grain; LENGTH is negligible longitudinally
        if (grain == GrainAxis.LENGTH) return abs(moistureNowPercent - equilibriumPercent) * c.radialPerPercent * widthInches * 0.1
        return abs(moistureNowPercent - equilibriumPercent) * rate * widthInches
    }

    fun acclimationNote(moistureNowPercent: Double, equilibriumPercent: Double): String {
        val delta = abs(moistureNowPercent - equilibriumPercent)
        return when {
            delta <= 2.0 -> "Stock is near equilibrium (ΔMC ${"%.1f".format(delta)}%) — OK to mill final"
            delta <= 5.0 -> "Allow further acclimation; ΔMC ${"%.1f".format(delta)}% before glue-up"
            else -> "Do not glue panels yet — acclimate until within ~2% of EMC (${equilibriumPercent}%)"
        }
    }

    fun breadboardGuidance(panelWidthInches: Double): String =
        "Breadboard ends: elongate pin holes toward panel edges; leave ${"%.2f".format(panelWidthInches * 0.01)}–${"%.2f".format(panelWidthInches * 0.02)} in seasonal gap each side"

    /**
     * Gate for Phase 17: rigid cross-grain attach that fights movement → WARN/BLOCK.
     */
    fun joineryMovementGate(
        request: JointRequest,
        panelWidthInches: Double = 24.0,
        moistureNowPercent: Double = 12.0,
        equilibriumPercent: Double = 8.0,
        useTangential: Boolean = true
    ): MoistureGateResult {
        val movement = expectedMovementInches(
            species = request.speciesA,
            widthInches = panelWidthInches,
            moistureNowPercent = moistureNowPercent,
            equilibriumPercent = equilibriumPercent,
            grain = GrainAxis.WIDTH,
            useTangential = useTangential
        )

        if (!request.crossGrainRigidAttach) {
            return MoistureGateResult(
                safe = true,
                severity = MoistureSeverity.OK,
                message = "Attachment allows movement or is with-grain",
                expectedMovementIn = movement
            )
        }

        // Rigid cross-grain (glued breadboard, screwed through tabletop without slots)
        return when {
            movement >= 0.15 -> MoistureGateResult(
                safe = false,
                severity = MoistureSeverity.BLOCK,
                message = "Unsafe cross-grain rigid attach: ~${"%.3f".format(movement)} in seasonal move — use slots/figure-8/breadboard pins",
                expectedMovementIn = movement
            )
            movement >= 0.05 -> MoistureGateResult(
                safe = false,
                severity = MoistureSeverity.WARN,
                message = "Cross-grain attach fights movement (~${"%.3f".format(movement)} in) — prefer floating fasteners",
                expectedMovementIn = movement
            )
            else -> MoistureGateResult(
                safe = true,
                severity = MoistureSeverity.OK,
                message = "Cross-grain move small (~${"%.3f".format(movement)} in) for this width/MC",
                expectedMovementIn = movement
            )
        }
    }

    fun panelGlueUpNote(species: String, boardWidths: List<Double>): String {
        val total = boardWidths.sum()
        val move = expectedMovementInches(species, total, 12.0, 8.0, useTangential = true)
        return "Glue-up width ${"%.1f".format(total)} in (${species}): expect ~${"%.3f".format(move)} in tangential move across panel — alternate growth rings"
    }
}
