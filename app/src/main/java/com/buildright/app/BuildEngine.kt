package com.buildright.app

import kotlin.math.ceil
import kotlin.math.sqrt

/** Pure calculation layer. No Android UI dependencies. */
data class EngineLine(
    val category: String,
    val name: String,
    val quantity: Double,
    val unit: String,
    val detail: String = ""
)

data class CutLine(
    val system: String,
    val member: String,
    val count: Int,
    val cutLengthFt: Double,
    val note: String = ""
)

data class WallSummary(
    val wall: String,
    val lengthFt: Double,
    val grossAreaSqFt: Double,
    val openingAreaSqFt: Double,
    val layoutStuds: Int,
    val extraOpeningStuds: Int
)

data class ShedEngineResult(
    val floorAreaSqFt: Double,
    val wallAreaGrossSqFt: Double,
    val wallAreaNetSqFt: Double,
    val roofAreaSqFt: Double,
    val floorJoistCount: Int,
    val wallStudCount: Int,
    val rafterCount: Int,
    val rafterLengthFt: Double,
    val ridgeLengthFt: Double,
    val foundationSupportCount: Int,
    val lines: List<EngineLine>,
    val cutList: List<CutLine>,
    val walls: List<WallSummary>,
    val warnings: List<String>
)

object BuildEngine {
    fun calculateShed(project: Project): ShedEngineResult {
        if (project.width <= 0 || project.length <= 0 || project.height <= 0) {
            return ShedEngineResult(0.0,0.0,0.0,0.0,0,0,0,0.0,0.0,0,emptyList(),emptyList(),emptyList(),listOf("Enter width, length and wall height to calculate the shed."))
        }

        val widthFt = project.width
        val lengthFt = project.length
        val heightFt = project.height
        val waste = 1.0 + project.wastePercent.coerceIn(0.0, 50.0) / 100.0
        val studSpacing = project.studSpacingIn.coerceAtLeast(1.0)
        val joistSpacing = project.joistSpacingIn.coerceAtLeast(1.0)
        val rafterSpacing = project.rafterSpacingIn.coerceAtLeast(1.0)
        val stockLength = project.stockLengthFt.coerceAtLeast(8.0)

        val floorArea = widthFt * lengthFt
        val perimeter = 2.0 * (widthFt + lengthFt)
        val grossWallArea = perimeter * heightFt
        val doorArea = project.doorWidthFt.coerceAtLeast(0.0) * project.doorHeightFt.coerceAtLeast(0.0)
        val windowAreaEach = project.windowWidthFt.coerceAtLeast(0.0) * project.windowHeightFt.coerceAtLeast(0.0)
        val totalWindowArea = project.windowCount.coerceAtLeast(0) * windowAreaEach
        val netWallArea = (grossWallArea - doorArea - totalWindowArea).coerceAtLeast(0.0)

        val walls = listOf(
            wallSummary("Front", lengthFt, heightFt, studSpacing, project, doorArea, windowAreaEach),
            wallSummary("Rear", lengthFt, heightFt, studSpacing, project, doorArea, windowAreaEach),
            wallSummary("Left", widthFt, heightFt, studSpacing, project, doorArea, windowAreaEach),
            wallSummary("Right", widthFt, heightFt, studSpacing, project, doorArea, windowAreaEach)
        )
        val wallStuds = walls.sumOf { it.layoutStuds + it.extraOpeningStuds } + 8 // three-stud style corner allowance

        // Joists span width and are laid out along length.
        val joists = ceil(lengthFt * 12.0 / joistSpacing).toInt() + 1

        val eave = project.eaveOverhangFt.coerceAtLeast(0.0)
        val rake = project.rakeOverhangFt.coerceAtLeast(0.0)
        val roofRun = if (project.roofStyle.equals("Shed", true)) widthFt + eave else (widthFt / 2.0) + eave
        val rise = roofRun * (project.roofPitchRise.coerceAtLeast(0.0) / 12.0)
        val rafterLength = sqrt(roofRun * roofRun + rise * rise)
        val roofLength = lengthFt + 2.0 * rake
        val rafterPositions = ceil(roofLength * 12.0 / rafterSpacing).toInt() + 1
        val rafters = if (project.roofStyle.equals("Shed", true)) rafterPositions else rafterPositions * 2
        val roofArea = if (project.roofStyle.equals("Shed", true)) rafterLength * roofLength else 2.0 * rafterLength * roofLength
        val ridgeLength = if (project.roofStyle.equals("Gable", true)) roofLength else 0.0

        val floorSheets = ceil((floorArea * waste) / 32.0)
        val wallSheets = ceil((netWallArea * waste) / 32.0)
        val roofSheets = ceil((roofArea * waste) / 32.0)
        val roofingSquares = roofArea * waste / 100.0
        val sidingSqFt = netWallArea * waste
        val rimLinearFt = perimeter
        val plateStockPieces = walls.sumOf { ceil(it.lengthFt / stockLength).toInt() * 3 }

        val foundationSupports = when (project.foundationType.lowercase()) {
            "concrete slab" -> 0
            "skids" -> project.skidCount.coerceAtLeast(2)
            else -> {
                val across = ceil(widthFt / project.foundationSpacingFt.coerceAtLeast(2.0)).toInt() + 1
                val along = ceil(lengthFt / project.foundationSpacingFt.coerceAtLeast(2.0)).toInt() + 1
                across * along
            }
        }

        val lines = mutableListOf<EngineLine>()
        when (project.foundationType.lowercase()) {
            "concrete slab" -> {
                val slabThicknessFt = project.slabThicknessIn.coerceAtLeast(2.0) / 12.0
                val cubicYards = floorArea * slabThicknessFt / 27.0 * waste
                lines += EngineLine("Foundation", "Concrete slab", cubicYards, "cu yd", "${fmt(project.slabThicknessIn)} in nominal thickness; base/rebar not included")
            }
            "skids" -> lines += EngineLine("Foundation", "${project.skidSize} skids", foundationSupports.toDouble(), "pcs", "Run along shed length; verify support/bearing requirements")
            else -> lines += EngineLine("Foundation", "Pier / block supports", foundationSupports.toDouble(), "supports", "Planning grid at approx. ${fmt(project.foundationSpacingFt)} ft spacing")
        }
        lines += listOf(
            EngineLine("Floor", "${project.floorJoistSize} floor joists", joists.toDouble(), "pcs", "Approx. ${fmt(widthFt)} ft cut/span at ${fmt(joistSpacing)} in O.C."),
            EngineLine("Floor", "${project.floorJoistSize} rim / band framing", rimLinearFt * waste, "linear ft"),
            EngineLine("Floor", "3/4 in floor sheathing 4×8", floorSheets, "sheets", "Includes ${fmt(project.wastePercent)}% waste"),
            EngineLine("Walls", "${project.studSize} wall studs", wallStuds.toDouble(), "pcs", "Wall-by-wall layout + corners/opening allowance"),
            EngineLine("Walls", "${project.studSize} plate stock", plateStockPieces.toDouble(), "${fmt(stockLength)} ft pcs", "Bottom + double top plate; simple stock segmentation"),
            EngineLine("Walls", "Wall sheathing 4×8", wallSheets, "sheets", "Net of entered openings + waste"),
            EngineLine("Walls", "Siding coverage", sidingSqFt, "sq ft", "Planning coverage"),
            EngineLine("Roof", "${project.rafterSize} rafters", rafters.toDouble(), "pcs", "Approx. ${fmt(rafterLength)} ft each at ${fmt(rafterSpacing)} in O.C."),
            EngineLine("Roof", "${project.ridgeBoardSize} ridge board", ridgeLength * waste, "linear ft", if (ridgeLength == 0.0) "Not used for shed roof" else "Gable ridge planning quantity"),
            EngineLine("Roof", "Roof sheathing 4×8", roofSheets, "sheets", "Includes overhangs + ${fmt(project.wastePercent)}% waste"),
            EngineLine("Roof", "Roofing coverage", roofingSquares, "squares", "1 square = 100 sq ft"),
            EngineLine("Hardware", "Framing connectors / fasteners", 1.0, "allowance", "Connection schedule must be verified for the project"),
            EngineLine("Weather", "Housewrap / wall WRB", netWallArea * waste, "sq ft"),
            EngineLine("Weather", "Roof underlayment", roofArea * waste, "sq ft")
        )

        val cutList = mutableListOf<CutLine>()
        cutList += CutLine("Floor", project.floorJoistSize, joists, widthFt, "Floor joists; confirm bearing/detail")
        cutList += CutLine("Walls", project.studSize, wallStuds, heightFt, "Stud takeoff includes opening/corner allowance; headers/jacks need field/design verification")
        walls.forEach { wall ->
            cutList += CutLine("Wall plates", project.studSize, ceil(wall.lengthFt / stockLength).toInt() * 3, stockLength, "${wall.wall}: stock pieces to create bottom + double top plates")
        }
        cutList += CutLine("Roof", project.rafterSize, rafters, rafterLength, "Rafter length is geometric before birdsmouth/tail/ridge cuts")
        if (ridgeLength > 0) cutList += CutLine("Roof", project.ridgeBoardSize, ceil(ridgeLength / stockLength).toInt(), stockLength, "Ridge stock; splice/support rules require verification")

        val warnings = buildList {
            add("Planning/takeoff engine only: structural member sizing, spans, footings, headers, anchors and connections require project-specific verification.")
            add("Cut list lengths are geometric/stock-planning values and do not include saw kerf, birdsmouth, plumb cuts, lap details or local framing requirements.")
            if (project.doorWall !in listOf("Front","Rear","Left","Right")) add("Door wall selection is invalid; choose Front, Rear, Left or Right.")
            if (project.windowWall !in listOf("Front","Rear","Left","Right")) add("Window wall selection is invalid; choose Front, Rear, Left or Right.")
            if (project.studSpacingIn < 12.0 || project.joistSpacingIn < 12.0 || project.rafterSpacingIn < 12.0) add("One or more framing spacings are under 12 inches O.C.; BuildRight used the entered value, but verify that spacing is intentional.")
            if (widthFt > 16.0) add("The entered floor/roof span is large enough that member sizing should be verified before purchasing lumber.")
            if (doorArea + totalWindowArea >= grossWallArea) add("Entered opening area is unusually large relative to wall area; verify dimensions and wall assignment.")
        }

        return ShedEngineResult(floorArea,grossWallArea,netWallArea,roofArea,joists,wallStuds,rafters,rafterLength,ridgeLength,foundationSupports,lines,cutList,walls,warnings)
    }

    private fun wallSummary(
        name: String,
        length: Double,
        height: Double,
        spacingIn: Double,
        p: Project,
        doorArea: Double,
        windowAreaEach: Double
    ): WallSummary {
        val layout = ceil(length * 12.0 / spacingIn).toInt() + 1
        val doorOnWall = p.doorWall.equals(name, true) && p.doorWidthFt > 0 && p.doorHeightFt > 0
        val windowsOnWall = if (p.windowWall.equals(name, true)) p.windowCount.coerceAtLeast(0) else 0
        val openingArea = (if (doorOnWall) doorArea else 0.0) + windowsOnWall * windowAreaEach
        val extras = (if (doorOnWall) 4 else 0) + windowsOnWall * 4
        return WallSummary(name,length,length*height,openingArea,layout,extras)
    }
}
