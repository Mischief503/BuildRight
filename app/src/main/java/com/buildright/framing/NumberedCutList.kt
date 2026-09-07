package com.buildright.framing

data class CutListRow(
    val number: Int,
    val kind: MemberKind,
    val size: String,
    val lengthInches: Double,
    val location: String?,
    val note: String
)

object NumberedCutList {
    fun create(plan: FramingPlan): List<CutListRow> {
        var n = 1
        return plan.members.flatMap { m ->
            (1..m.quantity).map {
                CutListRow(n++, m.kind, m.nominalSize, m.lengthInches, m.wall, m.note)
            }
        }
    }
}
