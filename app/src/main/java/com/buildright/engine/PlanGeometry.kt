package com.buildright.engine

import com.buildright.model.WallOpening
import com.buildright.model.WallSide

data class PlanPoint(val x: Float, val y: Float)
data class PlanRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

object PlanGeometry {
    fun wallLength(side: WallSide, widthFt: Double, lengthFt: Double): Double =
        when (side) {
            WallSide.FRONT, WallSide.REAR -> widthFt
            WallSide.LEFT, WallSide.RIGHT -> lengthFt
        }

    fun validateOpening(
        opening: WallOpening,
        widthFt: Double,
        lengthFt: Double,
        wallHeightFt: Double
    ): List<String> {
        val errors = mutableListOf<String>()
        val wallLength = wallLength(opening.wall, widthFt, lengthFt)
        if (opening.offsetFt < 0) errors += "Opening offset cannot be negative."
        if (opening.widthFt <= 0 || opening.heightFt <= 0) errors += "Opening dimensions must be greater than zero."
        if (opening.offsetFt + opening.widthFt > wallLength) errors += "Opening extends past the end of the wall."
        if (opening.sillHeightFt + opening.heightFt > wallHeightFt) errors += "Opening extends above wall height."
        return errors
    }

    fun openingRect(
        opening: WallOpening,
        wallLengthFt: Double,
        wallHeightFt: Double,
        canvasWidth: Float,
        canvasHeight: Float
    ): PlanRect {
        val xScale = canvasWidth / wallLengthFt.toFloat().coerceAtLeast(0.001f)
        val yScale = canvasHeight / wallHeightFt.toFloat().coerceAtLeast(0.001f)
        val left = (opening.offsetFt * xScale).toFloat()
        val right = ((opening.offsetFt + opening.widthFt) * xScale).toFloat()
        val bottom = canvasHeight - (opening.sillHeightFt * yScale).toFloat()
        val top = bottom - (opening.heightFt * yScale).toFloat()
        return PlanRect(left, top, right, bottom)
    }
}
