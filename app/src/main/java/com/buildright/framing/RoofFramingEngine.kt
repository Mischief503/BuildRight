package com.buildright.framing

import kotlin.math.ceil
import kotlin.math.sqrt

object RoofFramingEngine {
    fun gable(
        widthFt: Double,
        lengthFt: Double,
        pitchRise: Double,
        rafterSpacingInches: Int,
        rafterSize: String,
        ridgeSize: String,
        eaveOverhangFt: Double,
        rakeOverhangFt: Double
    ): FramingPlan {
        val m = mutableListOf<FramingMember>()
        val warnings = mutableListOf<FramingWarning>()

        val halfRunIn = widthFt * 12.0 / 2.0
        val riseIn = halfRunIn * (pitchRise / 12.0)
        val slopeIn = sqrt(halfRunIn * halfRunIn + riseIn * riseIn)
        val overhangSlopeIn = eaveOverhangFt * 12.0 * sqrt(1.0 + (pitchRise/12.0)*(pitchRise/12.0))
        val approximateRafterLength = slopeIn + overhangSlopeIn
        val roofLengthIn = (lengthFt + rakeOverhangFt * 2) * 12.0
        val pairs = ceil(roofLengthIn / rafterSpacingInches).toInt() + 1

        m += FramingMember("ridge", MemberKind.RIDGE_BOARD, ridgeSize, roofLengthIn)
        repeat(pairs) { i ->
            m += FramingMember("rafter-L-${i+1}", MemberKind.RAFTER, rafterSize, approximateRafterLength, note="Pre-cut geometric length; field/layout details required.")
            m += FramingMember("rafter-R-${i+1}", MemberKind.RAFTER, rafterSize, approximateRafterLength, note="Pre-cut geometric length; field/layout details required.")
        }
        m += FramingMember("fascia-L", MemberKind.FASCIA, rafterSize, roofLengthIn)
        m += FramingMember("fascia-R", MemberKind.FASCIA, rafterSize, roofLengthIn)

        warnings += FramingWarning(
            "RAFTER_CUT_DETAIL",
            "Rafter length is geometric. Ridge deduction, birdsmouth, seat cut, tail/plumb cuts and structural limits require validated detail rules."
        )
        return FramingPlan(m, warnings)
    }

    fun roofSheathingPanels(
        widthFt: Double,
        lengthFt: Double,
        pitchRise: Double,
        eaveOverhangFt: Double,
        rakeOverhangFt: Double,
        wastePercent: Double
    ): Int {
        val totalWidth = widthFt + 2 * eaveOverhangFt
        val totalLength = lengthFt + 2 * rakeOverhangFt
        val slopeFactor = sqrt(1.0 + (pitchRise / 12.0) * (pitchRise / 12.0))
        val roofArea = totalWidth * totalLength * slopeFactor
        return ceil((roofArea / 32.0) * (1.0 + wastePercent / 100.0)).toInt()
    }
}
