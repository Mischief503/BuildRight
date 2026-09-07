package com.buildright.framing

import com.buildright.model.WallOpening
import com.buildright.model.WallSide
import kotlin.math.ceil

object WallFramingEngine {
    fun generate(
        wall: WallSide,
        wallLengthFt: Double,
        wallHeightFt: Double,
        studSpacingInches: Int,
        studSize: String,
        openings: List<WallOpening>,
        headerSize: String? = null
    ): FramingPlan {
        val members = mutableListOf<FramingMember>()
        val warnings = mutableListOf<FramingWarning>()
        val wallIn = wallLengthFt * 12.0
        val heightIn = wallHeightFt * 12.0
        val wallName = wall.name

        members += FramingMember("$wallName-bottom", MemberKind.BOTTOM_PLATE, studSize, wallIn, wall = wallName)
        members += FramingMember("$wallName-top1", MemberKind.TOP_PLATE, studSize, wallIn, wall = wallName)
        members += FramingMember("$wallName-top2", MemberKind.TOP_PLATE, studSize, wallIn, wall = wallName)

        // Three-stud corner packs at each end; exact corner strategy can become configurable later.
        members += FramingMember("$wallName-corner-L", MemberKind.CORNER_STUD, studSize, heightIn, 3, wallName, "Left corner pack")
        members += FramingMember("$wallName-corner-R", MemberKind.CORNER_STUD, studSize, heightIn, 3, wallName, "Right corner pack")

        val relevant = openings.filter { it.wall == wall }.sortedBy { it.offsetFt }
        relevant.forEachIndexed { i, o ->
            members += FramingMember("$wallName-open-$i-kingL", MemberKind.KING_STUD, studSize, heightIn, wall = wallName)
            members += FramingMember("$wallName-open-$i-kingR", MemberKind.KING_STUD, studSize, heightIn, wall = wallName)
            val jackLen = (o.sillHeightFt + o.heightFt) * 12.0
            if (jackLen > 0) {
                members += FramingMember("$wallName-open-$i-jackL", MemberKind.JACK_STUD, studSize, jackLen, wall = wallName)
                members += FramingMember("$wallName-open-$i-jackR", MemberKind.JACK_STUD, studSize, jackLen, wall = wallName)
            }
            if (headerSize == null) {
                warnings += FramingWarning(
                    "HEADER_SIZE_REQUIRED",
                    "${o.label.ifBlank { o.type.name }} on $wallName needs a header size selected from validated structural/code data."
                )
            } else {
                val headerLen = o.widthFt * 12.0 + 3.0
                members += FramingMember("$wallName-open-$i-header", MemberKind.HEADER, headerSize, headerLen, 2, wallName)
            }
        }

        val bays = ceil(wallIn / studSpacingInches).toInt()
        for (i in 1 until bays) {
            val xIn = i * studSpacingInches.toDouble()
            val insideOpening = relevant.any { o ->
                val start = o.offsetFt * 12.0
                val end = (o.offsetFt + o.widthFt) * 12.0
                xIn > start && xIn < end
            }
            if (!insideOpening) {
                members += FramingMember("$wallName-stud-$i", MemberKind.STUD, studSize, heightIn, wall = wallName)
            }
        }
        return FramingPlan(members, warnings)
    }
}
