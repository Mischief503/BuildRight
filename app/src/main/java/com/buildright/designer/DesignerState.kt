package com.buildright.designer

import com.buildright.model.WallOpening

data class DesignerState(
    val view: DesignerView = DesignerView.FLOOR,
    val selection: DesignerSelection = DesignerSelection(),
    val showFraming: Boolean = true,
    val showDimensions: Boolean = true,
    val showLabels: Boolean = true,
    val snapInches: Int = 1,
    val openings: List<WallOpening> = emptyList()
)
