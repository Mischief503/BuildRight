package com.buildright.designer

import com.buildright.model.WallOpening
import com.buildright.model.WallSide
import kotlin.math.ceil

object DesignerGeometry {

    fun feetToPx(feet: Double, totalFeet: Double, availablePx: Float): Float {
        if (totalFeet <= 0.0) return 0f
        return (feet / totalFeet * availablePx).toFloat()
    }

    fun floorJoists(
        widthFt: Double,
        lengthFt: Double,
        spacingInches: Int,
        viewport: DesignerViewport
    ): List<LineSegment> {
        if (widthFt <= 0 || lengthFt <= 0 || spacingInches <= 0) return emptyList()
        val usableW = viewport.widthPx - viewport.paddingPx * 2
        val usableH = viewport.heightPx - viewport.paddingPx * 2
        val spacingFt = spacingInches / 12.0
        val count = ceil(lengthFt / spacingFt).toInt() + 1
        return (0 until count).map { index ->
            val posFt = (index * spacingFt).coerceAtMost(lengthFt)
            val y = viewport.paddingPx + feetToPx(posFt, lengthFt, usableH)
            LineSegment(
                viewport.paddingPx, y,
                viewport.paddingPx + usableW, y,
                "J${index + 1}"
            )
        }
    }

    fun wallStuds(
        wall: WallSide,
        wallLengthFt: Double,
        wallHeightFt: Double,
        spacingInches: Int,
        openings: List<WallOpening>,
        viewport: DesignerViewport
    ): List<LineSegment> {
        if (wallLengthFt <= 0 || wallHeightFt <= 0 || spacingInches <= 0) return emptyList()
        val usableW = viewport.widthPx - viewport.paddingPx * 2
        val usableH = viewport.heightPx - viewport.paddingPx * 2
        val spacingFt = spacingInches / 12.0
        val count = ceil(wallLengthFt / spacingFt).toInt() + 1
        val wallOpenings = openings.filter { it.wall == wall }

        return (0 until count).mapNotNull { index ->
            val xFt = (index * spacingFt).coerceAtMost(wallLengthFt)
            val blocked = wallOpenings.any { xFt > it.offsetFt && xFt < it.offsetFt + it.widthFt }
            if (blocked) null else {
                val x = viewport.paddingPx + feetToPx(xFt, wallLengthFt, usableW)
                LineSegment(x, viewport.paddingPx, x, viewport.paddingPx + usableH, "S${index + 1}")
            }
        }
    }

    fun moveOpening(
        opening: WallOpening,
        newCenterPx: Float,
        wallLengthFt: Double,
        viewportWidthPx: Float,
        paddingPx: Float
    ): WallOpening {
        val usable = (viewportWidthPx - paddingPx * 2).coerceAtLeast(1f)
        val normalized = ((newCenterPx - paddingPx) / usable).coerceIn(0f, 1f)
        val centerFt = normalized * wallLengthFt
        val maxOffset = (wallLengthFt - opening.widthFt).coerceAtLeast(0.0)
        val newOffset = (centerFt - opening.widthFt / 2.0).coerceIn(0.0, maxOffset)
        return opening.copy(offsetFt = newOffset)
    }

    fun overallDimensions(
        widthFt: Double,
        lengthFt: Double,
        viewport: DesignerViewport
    ): List<DimensionLine> {
        val p = viewport.paddingPx
        return listOf(
            DimensionLine(p, viewport.heightPx - 16f, viewport.widthPx - p, viewport.heightPx - 16f, "${widthFt} ft"),
            DimensionLine(16f, p, 16f, viewport.heightPx - p, "${lengthFt} ft")
        )
    }
}
