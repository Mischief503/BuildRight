package com.buildright.designer

import com.buildright.model.WallSide

enum class DesignerView { FLOOR, FRONT, REAR, LEFT, RIGHT, ROOF }
enum class SelectionType { NONE, WALL, OPENING, FRAMING_MEMBER }

data class DesignerSelection(
    val type: SelectionType = SelectionType.NONE,
    val id: String? = null,
    val wall: WallSide? = null
)

data class DimensionLine(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val label: String
)

data class LineSegment(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val label: String = ""
)

data class DesignerViewport(
    val widthPx: Float,
    val heightPx: Float,
    val paddingPx: Float = 48f
)
