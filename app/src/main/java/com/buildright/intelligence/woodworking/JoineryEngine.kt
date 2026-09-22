package com.buildright.intelligence.woodworking

/**
 * PRIOR-CHAT RECOVERY: Phase 17 Advanced Joinery engine.
 * Recovered test states: AUTO=MORTISE_TENON; provisional YELLOW; verified GREEN;
 * impossible tenon HOLD/RED.
 * IMPLEMENTED: recommendations, load/racking paths, geometry validation,
 * adhesive/fastener gates; integrates Phase 18 moisture movement gate.
 */
object JoineryEngine {

    fun recommend(request: JointRequest, moistureGate: MoistureGateResult? = null): JointRecommendation {
        val reasons = mutableListOf<String>()
        val impossible = isImpossibleTenon(request)
        val preferredOrAuto = request.preferred ?: autoSelect(request)

        if (impossible && (preferredOrAuto == JointType.MORTISE_TENON || preferredOrAuto == JointType.LOOSE_TENON)) {
            reasons += "Impossible tenon: tenon thickness/length exceeds member capacity — HOLD"
            val alt = JointType.DOWEL
            return JointRecommendation(
                requestId = request.id,
                recommended = alt,
                autoMarker = "AUTO=${preferredOrAuto.name}",
                safety = JointSafety.RED,
                hold = true,
                reasons = reasons + "Suggest ${alt.name} or reduce tenon to ≤1/3 of thinner member",
                adhesiveOk = false,
                fastenerOk = true,
                loadPath = request.loadPath
            )
        }

        val baseSafety = when {
            impossible -> JointSafety.RED
            request.verified -> JointSafety.GREEN
            else -> JointSafety.YELLOW
        }

        // Phase 18 integration: movement-fighting joints escalate safety
        var safety = baseSafety
        var hold = impossible
        var adhesiveOk = preferredOrAuto != JointType.BOLTED && preferredOrAuto != JointType.SCREWED
        var fastenerOk = preferredOrAuto == JointType.BOLTED || preferredOrAuto == JointType.SCREWED ||
            preferredOrAuto == JointType.DOWEL

        if (moistureGate != null && !moistureGate.safe) {
            reasons += moistureGate.message
            when (moistureGate.severity) {
                MoistureSeverity.BLOCK -> {
                    safety = JointSafety.RED
                    hold = true
                    adhesiveOk = false
                }
                MoistureSeverity.WARN -> {
                    if (safety == JointSafety.GREEN) safety = JointSafety.YELLOW
                    reasons += "Movement-safe attachment required (slots / figure-8 / breadboard)"
                }
                MoistureSeverity.OK -> Unit
            }
        }

        when (preferredOrAuto) {
            JointType.MORTISE_TENON -> reasons += "Mortise-and-tenon preferred for racking resistance at leg/apron"
            JointType.LOOSE_TENON -> reasons += "Loose tenon (domino-style) when aligned mortises are practical"
            JointType.DOWEL -> reasons += "Dowels acceptable for light shear with accurate jigs"
            JointType.HALF_LAP -> reasons += "Half-lap for stretcher/rail intersections with adequate section"
            JointType.BOLTED -> reasons += "Bolted joint for knockdown / high clamp load"
            JointType.SCREWED -> reasons += "Screwed joint provisional only — verify withdrawal and grain"
        }

        if (request.verified && safety == JointSafety.GREEN) {
            reasons += "Shop-verified geometry — GREEN"
        } else if (safety == JointSafety.YELLOW) {
            reasons += "Provisional — verify layout before glue-up (YELLOW)"
        }

        return JointRecommendation(
            requestId = request.id,
            recommended = preferredOrAuto,
            autoMarker = "AUTO=${preferredOrAuto.name}",
            safety = safety,
            hold = hold,
            reasons = reasons,
            adhesiveOk = adhesiveOk && !hold,
            fastenerOk = fastenerOk,
            loadPath = request.loadPath
        )
    }

    /** AUTO selection: racking/structural furniture defaults to mortise-and-tenon. */
    fun autoSelect(request: JointRequest): JointType {
        return when (request.loadPath) {
            LoadPath.RACKING, LoadPath.MIXED -> JointType.MORTISE_TENON
            LoadPath.SHEAR -> JointType.HALF_LAP
            LoadPath.TENSION -> JointType.BOLTED
            LoadPath.COMPRESSION -> JointType.DOWEL
        }
    }

    /**
     * Impossible tenon: thickness > 1/3 of thinner member, or length > 5× thickness,
     * or tenon thicker than either member, or non-positive dimensions.
     */
    fun isImpossibleTenon(request: JointRequest): Boolean {
        val tenonT = request.tenonThicknessIn ?: return false
        val tenonL = request.tenonLengthIn ?: 0.0
        val thinner = minOf(request.memberAThicknessIn, request.memberBThicknessIn)
        if (tenonT <= 0 || request.memberAThicknessIn <= 0 || request.memberBThicknessIn <= 0) return true
        if (tenonT > thinner) return true
        if (tenonT > thinner / 3.0 + 1e-6) return true
        if (tenonL > 0 && tenonL > tenonT * 5.0) return true
        return false
    }

    fun toRecord(rec: JointRecommendation, memberA: String = "", memberB: String = ""): FurnitureJointRecord =
        FurnitureJointRecord(
            id = rec.requestId,
            jointType = rec.recommended.name,
            safety = rec.safety.name,
            autoMarker = rec.autoMarker,
            hold = rec.hold,
            notes = rec.reasons.joinToString("; "),
            memberA = memberA,
            memberB = memberB
        )
}
