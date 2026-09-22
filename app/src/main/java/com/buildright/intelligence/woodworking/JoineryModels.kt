package com.buildright.intelligence.woodworking

/**
 * PRIOR-CHAT RECOVERY: Phase 17 Advanced Joinery (v2.7.0 / VC36).
 * IMPLEMENTED: joint types, safety bands, recommendation result.
 */

enum class JointType {
    MORTISE_TENON,
    LOOSE_TENON,
    DOWEL,
    HALF_LAP,
    BOLTED,
    SCREWED
}

enum class JointSafety {
    /** Provisional — geometry plausible, verification incomplete. */
    YELLOW,
    /** Verified — geometry and load path OK. */
    GREEN,
    /** HOLD / impossible — do not proceed. */
    RED
}

enum class LoadPath {
    COMPRESSION,
    TENSION,
    SHEAR,
    RACKING,
    MIXED
}

data class JointRequest(
    val id: String,
    val memberAThicknessIn: Double,
    val memberBThicknessIn: Double,
    val tenonThicknessIn: Double? = null,
    val tenonLengthIn: Double? = null,
    val tenonWidthIn: Double? = null,
    val loadPath: LoadPath = LoadPath.RACKING,
    val preferred: JointType? = null,
    /** When true, geometry has been shop-verified. */
    val verified: Boolean = false,
    /** Cross-grain rigid attach — Phase 18 may escalate. */
    val crossGrainRigidAttach: Boolean = false,
    val speciesA: String = "oak",
    val speciesB: String = "oak"
)

data class JointRecommendation(
    val requestId: String,
    val recommended: JointType,
    /** Historical test marker form: AUTO=MORTISE_TENON */
    val autoMarker: String,
    val safety: JointSafety,
    val hold: Boolean,
    val reasons: List<String>,
    val adhesiveOk: Boolean,
    val fastenerOk: Boolean,
    val loadPath: LoadPath
)

data class FurnitureJointRecord(
    val id: String,
    val jointType: String,
    val safety: String,
    val autoMarker: String,
    val hold: Boolean,
    val notes: String = "",
    val memberA: String = "",
    val memberB: String = ""
)
