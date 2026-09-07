package com.buildright.framing

object CutOptimizer {
    /**
     * First-fit decreasing stock optimization for members of one nominal size.
     * Kerf is included between cuts. Quantities are expanded into individual pieces.
     */
    fun optimize(
        members: List<FramingMember>,
        stockLengthInches: Double,
        kerfInches: Double = 0.125
    ): List<StockBoard> {
        val pieces = members.flatMap { m ->
            (1..m.quantity).map { m.copy(id = "${m.id}-$it", quantity = 1) }
        }.sortedByDescending { it.lengthInches }

        val boards = mutableListOf<StockBoard>()
        pieces.forEach { piece ->
            val board = boards.firstOrNull { b ->
                val kerfs = if (b.cuts.isEmpty()) 0.0 else kerfInches
                b.usedInches + kerfs + piece.lengthInches <= b.stockLengthInches
            }
            if (board != null) board.cuts += piece
            else if (piece.lengthInches <= stockLengthInches) {
                boards += StockBoard(piece.nominalSize, stockLengthInches, mutableListOf(piece))
            }
        }
        return boards
    }
}
