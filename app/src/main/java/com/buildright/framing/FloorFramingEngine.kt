package com.buildright.framing

import kotlin.math.ceil

object FloorFramingEngine {
    fun generate(
        widthFt: Double,
        lengthFt: Double,
        joistSpacingInches: Int,
        joistSize: String,
        blockingRows: Int = 1
    ): FramingPlan {
        val m = mutableListOf<FramingMember>()
        val warnings = mutableListOf<FramingWarning>()
        val widthIn = widthFt * 12
        val lengthIn = lengthFt * 12
        val count = ceil(lengthIn / joistSpacingInches).toInt() + 1

        m += FramingMember("rim-front", MemberKind.RIM_BOARD, joistSize, widthIn, wall="FRONT")
        m += FramingMember("rim-rear", MemberKind.RIM_BOARD, joistSize, widthIn, wall="REAR")
        repeat(count) { i ->
            m += FramingMember("floor-joist-${i+1}", MemberKind.FLOOR_JOIST, joistSize, widthIn)
        }

        if (blockingRows > 0 && count > 1) {
            val clearBay = joistSpacingInches - 1.5
            repeat(blockingRows) { row ->
                m += FramingMember("blocking-row-${row+1}", MemberKind.BLOCKING, joistSize, clearBay, count - 1,
                    note="Field verify actual clear spacing before cutting.")
            }
        }

        warnings += FramingWarning(
            "JOIST_SPAN_NOT_VALIDATED",
            "Joist size/spacing is represented geometrically only; allowable span must be validated separately."
        )
        return FramingPlan(m, warnings)
    }
}
