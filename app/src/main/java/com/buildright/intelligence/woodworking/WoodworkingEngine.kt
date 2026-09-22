package com.buildright.intelligence.woodworking

/**
 * PRIOR-CHAT RECOVERY: Phase 16 Advanced Woodworking & Furniture Engineering (v2.6.0 / VC35).
 * Historical ZIP bytes unavailable; reconstruct from Gate A Chunk 01 spec.
 * IMPLEMENTED: table-base modeling, cut-list (NumberedCutList/CutOptimizer patterns),
 * stability hook (feeds Phase 19), joinery considerations (feeds Phase 17), build sequencing.
 */
object WoodworkingEngine {

    fun modelTableBase(spec: TableBaseSpec, stockLengthInches: Double = 96.0): FurniturePlan {
        val warnings = mutableListOf<String>()
        if (spec.topLengthInches <= 0 || spec.topWidthInches <= 0 || spec.baseHeightInches <= 0) {
            warnings += "Table dimensions must be positive"
            return FurniturePlan(
                members = emptyList(),
                panels = emptyList(),
                cutList = emptyList(),
                stockBoards = emptyList(),
                stability = StabilityHook(StabilityLevel.UNSTABLE, 0.0, 0.0, "Invalid geometry"),
                joineryHints = emptyList(),
                buildSequence = emptyList(),
                warnings = warnings
            )
        }

        val footprintL = (spec.topLengthInches - 2 * spec.overhangInches).coerceAtLeast(12.0)
        val footprintW = (spec.topWidthInches - 2 * spec.overhangInches).coerceAtLeast(12.0)
        val apronLong = footprintL
        val apronShort = footprintW - 2 * nominalToInches(spec.legSize)

        val members = listOf(
            WoodMember("leg", WoodMemberRole.LEG, spec.legSize, spec.baseHeightInches, quantity = 4, note = "table legs"),
            WoodMember("apron-l", WoodMemberRole.APRON, spec.apronSize, apronLong, quantity = 2, note = "long aprons"),
            WoodMember("apron-s", WoodMemberRole.APRON, spec.apronSize, apronShort.coerceAtLeast(6.0), quantity = 2, note = "short aprons"),
            WoodMember("stretcher", WoodMemberRole.STRETCHER, "1×3", footprintW * 0.85, quantity = 1, note = "lower stretcher"),
            WoodMember("top-support", WoodMemberRole.TOP_SUPPORT, "1×2", footprintL * 0.9, quantity = 2, note = "floating top cleats")
        )

        val panels = listOf(
            WoodPanel(
                id = "slab-top",
                lengthInches = spec.topLengthInches,
                widthInches = spec.topWidthInches,
                thicknessInches = spec.topThicknessInches,
                grainAxis = GrainAxis.LENGTH,
                species = "granite/slab",
                note = "non-wood top; wooden base carries load"
            )
        )

        val cutList = numberedCutList(members)
        val stockBoards = optimizeCuts(members, stockLengthInches)
        val stability = evaluateBaseUnderSlab(spec, footprintL, footprintW)
        val joineryHints = listOf(
            "Prefer mortise-and-tenon at leg/apron intersections for racking resistance",
            "Use elongated slots or figure-8 fasteners at top supports for seasonal movement",
            "Stretcher may use dowels or half-laps if shop lacks mortising capacity"
        )
        val sequence = buildSequence()

        return FurniturePlan(
            members = members,
            panels = panels,
            cutList = cutList,
            stockBoards = stockBoards,
            stability = stability,
            joineryHints = joineryHints,
            buildSequence = sequence,
            warnings = warnings
        )
    }

    /**
     * Numbered cut-list — mirrors com.buildright.framing.NumberedCutList pattern.
     */
    fun numberedCutList(members: List<WoodMember>): List<WoodCutRow> {
        var n = 1
        return members.flatMap { m ->
            (1..m.quantity).map {
                WoodCutRow(n++, m.role, m.nominalSize, m.lengthInches, m.note)
            }
        }
    }

    /**
     * First-fit decreasing stock optimization — mirrors com.buildright.framing.CutOptimizer.
     */
    fun optimizeCuts(
        members: List<WoodMember>,
        stockLengthInches: Double,
        kerfInches: Double = 0.125
    ): List<WoodStockBoard> {
        val pieces = members.flatMap { m ->
            (1..m.quantity).map { m.copy(id = "${m.id}-$it", quantity = 1) }
        }.sortedByDescending { it.lengthInches }

        val boards = mutableListOf<WoodStockBoard>()
        pieces.forEach { piece ->
            val board = boards.firstOrNull { b ->
                val kerfs = if (b.cuts.isEmpty()) 0.0 else kerfInches
                b.usedInches + kerfs + piece.lengthInches <= b.stockLengthInches
            }
            if (board != null) board.cuts += piece
            else if (piece.lengthInches <= stockLengthInches) {
                boards += WoodStockBoard(piece.nominalSize, stockLengthInches, mutableListOf(piece))
            }
        }
        return boards
    }

    /**
     * Stability reasoning hook — feeds Phase 19 Furniture Load Paths & Stability later.
     * Checks tip resistance and footprint coverage under slab mass.
     */
    fun evaluateBaseUnderSlab(
        spec: TableBaseSpec,
        footprintL: Double = (spec.topLengthInches - 2 * spec.overhangInches).coerceAtLeast(12.0),
        footprintW: Double = (spec.topWidthInches - 2 * spec.overhangInches).coerceAtLeast(12.0)
    ): StabilityHook {
        val topArea = spec.topLengthInches * spec.topWidthInches
        val footArea = footprintL * footprintW
        val coverage = if (topArea <= 0) 0.0 else footArea / topArea
        // Tip factor: wider/taller bases resist tip; heavy tops need more footprint
        val tip = (footprintL * footprintW) / (spec.baseHeightInches * (spec.topWeightLbs / 50.0).coerceAtLeast(1.0))
        val level = when {
            coverage >= 0.55 && tip >= 2.0 -> StabilityLevel.STABLE
            coverage >= 0.40 && tip >= 1.0 -> StabilityLevel.MARGINAL
            else -> StabilityLevel.UNSTABLE
        }
        val message = when (level) {
            StabilityLevel.STABLE -> "Base footprint and tip resistance OK for slab load"
            StabilityLevel.MARGINAL -> "Reduce overhang or widen apron footprint before Phase 19 load paths"
            StabilityLevel.UNSTABLE -> "Base under-sized for slab; enlarge footprint or lower center of mass"
        }
        return StabilityHook(level, tip, coverage, message)
    }

    private fun buildSequence(): List<BuildStep> = listOf(
        BuildStep(1, "Mill stock", "Dimension legs, aprons, stretcher, and top cleats to final size"),
        BuildStep(2, "Cut joinery", "Lay out and cut mortises/tenons or alternate joints at leg/apron nodes"),
        BuildStep(3, "Dry assemble", "Clamp base without glue; check square and flatness"),
        BuildStep(4, "Glue & clamp", "Assemble base square; leave top cleats unglued to rails where slots allow movement"),
        BuildStep(5, "Fit top", "Attach slab with movement-safe fasteners; verify overhang and level"),
        BuildStep(6, "Finish", "Sand, finish wood; protect slab per material guidance")
    )

    /** Parse nominal lumber like "3×3" or "2x4" to approximate face inches. */
    fun nominalToInches(nominal: String): Double {
        val cleaned = nominal.lowercase().replace('×', 'x').replace(" ", "")
        val parts = cleaned.split('x')
        return parts.firstOrNull()?.toDoubleOrNull() ?: 1.5
    }
}
