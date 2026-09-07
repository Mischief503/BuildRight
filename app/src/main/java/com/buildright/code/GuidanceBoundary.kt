package com.buildright.code

enum class GuidanceType { CALCULATION, CODE_REFERENCE, STRUCTURAL_DECISION }

data class GuidanceNotice(
    val type: GuidanceType,
    val title: String,
    val message: String
)

object GuidanceBoundary {
    fun calculationNotice() = GuidanceNotice(
        GuidanceType.CALCULATION,
        "Calculated value",
        "This value is derived from project geometry or quantities."
    )

    fun codeNotice(jurisdiction: Jurisdiction) = GuidanceNotice(
        GuidanceType.CODE_REFERENCE,
        "Jurisdiction-specific code guidance",
        "This guidance is tied to ${jurisdiction.displayName()} and should be verified against the cited current source."
    )

    fun structuralNotice() = GuidanceNotice(
        GuidanceType.STRUCTURAL_DECISION,
        "Structural validation required",
        "Member sizing, connections, loads, spans and foundation adequacy require validated prescriptive tables or engineering data."
    )
}
