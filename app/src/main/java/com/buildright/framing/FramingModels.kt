package com.buildright.framing

enum class MemberKind {
    STUD, KING_STUD, JACK_STUD, CRIPPLE, HEADER,
    BOTTOM_PLATE, TOP_PLATE, CORNER_STUD,
    FLOOR_JOIST, RIM_BOARD, BLOCKING,
    RAFTER, RIDGE_BOARD, FASCIA
}

data class FramingMember(
    val id: String,
    val kind: MemberKind,
    val nominalSize: String,
    val lengthInches: Double,
    val quantity: Int = 1,
    val wall: String? = null,
    val note: String = ""
)

data class FramingWarning(
    val code: String,
    val message: String
)

data class FramingPlan(
    val members: List<FramingMember>,
    val warnings: List<FramingWarning>
)

data class StockBoard(
    val nominalSize: String,
    val stockLengthInches: Double,
    val cuts: MutableList<FramingMember> = mutableListOf()
) {
    val usedInches: Double get() = cuts.sumOf { it.lengthInches * it.quantity }
    val wasteInches: Double get() = stockLengthInches - usedInches
}
