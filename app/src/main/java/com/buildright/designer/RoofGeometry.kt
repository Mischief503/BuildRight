package com.buildright.designer

data class RoofPlan(
    val outerLeft: Float,
    val outerTop: Float,
    val outerRight: Float,
    val outerBottom: Float,
    val ridge: LineSegment?,
    val pitchLabel: String
)

object RoofGeometry {
    fun plan(
        widthFt: Double,
        lengthFt: Double,
        eaveOverhangFt: Double,
        rakeOverhangFt: Double,
        pitchRise: Double,
        isGable: Boolean,
        viewport: DesignerViewport
    ): RoofPlan {
        val totalW = widthFt + rakeOverhangFt * 2
        val totalL = lengthFt + eaveOverhangFt * 2
        val usableW = viewport.widthPx - viewport.paddingPx * 2
        val usableH = viewport.heightPx - viewport.paddingPx * 2
        val scale = minOf(usableW / totalW.toFloat().coerceAtLeast(.001f),
                          usableH / totalL.toFloat().coerceAtLeast(.001f))
        val drawW = totalW.toFloat() * scale
        val drawH = totalL.toFloat() * scale
        val left = (viewport.widthPx - drawW) / 2
        val top = (viewport.heightPx - drawH) / 2
        val ridge = if (isGable) {
            val x = left + drawW / 2
            LineSegment(x, top, x, top + drawH, "Ridge")
        } else null
        return RoofPlan(left, top, left + drawW, top + drawH, ridge, "${pitchRise}:12")
    }
}
