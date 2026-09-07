package com.buildright.designer

import com.buildright.model.OpeningType
import com.buildright.model.WallOpening
import com.buildright.model.WallSide

object OpeningCommands {
    fun newDoor(wall: WallSide, wallLengthFt: Double): WallOpening {
        val width = 3.0
        return WallOpening(
            type = OpeningType.DOOR,
            wall = wall,
            offsetFt = ((wallLengthFt - width) / 2.0).coerceAtLeast(0.0),
            widthFt = width,
            heightFt = 6.67,
            sillHeightFt = 0.0,
            label = "Door"
        )
    }

    fun newWindow(wall: WallSide, wallLengthFt: Double): WallOpening {
        val width = 3.0
        return WallOpening(
            type = OpeningType.WINDOW,
            wall = wall,
            offsetFt = ((wallLengthFt - width) / 2.0).coerceAtLeast(0.0),
            widthFt = width,
            heightFt = 3.0,
            sillHeightFt = 3.0,
            label = "Window"
        )
    }
}
