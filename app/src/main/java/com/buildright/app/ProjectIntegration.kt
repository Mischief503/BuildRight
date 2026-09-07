package com.buildright.app

import com.buildright.assistant.AssistantProjectSnapshot
import com.buildright.code.ProjectCodeContext
import com.buildright.cost.MaterialRequirement
import com.buildright.framing.FramingPlan
import com.buildright.framing.WallFramingEngine
import com.buildright.model.OpeningType
import com.buildright.model.WallOpening
import com.buildright.model.WallSide

/**
 * Bridge between the persisted v1 Project and the feature engines added in v0.4-v0.9.
 * The persisted Project remains the single authoritative source of truth.
 */
object ProjectIntegration {
    fun wallSide(name: String): WallSide = when (name.lowercase()) {
        "rear" -> WallSide.REAR
        "left" -> WallSide.LEFT
        "right" -> WallSide.RIGHT
        else -> WallSide.FRONT
    }

    fun openings(project: Project): List<WallOpening> {
        if (project.openings.isNotEmpty()) {
            return project.openings.map { opening ->
                WallOpening(
                    id = opening.id,
                    type = if (opening.type.equals("DOOR", true)) OpeningType.DOOR else OpeningType.WINDOW,
                    wall = wallSide(opening.wall),
                    offsetFt = opening.offsetFt,
                    widthFt = opening.widthFt,
                    heightFt = opening.heightFt,
                    sillHeightFt = opening.sillHeightFt,
                    label = opening.label
                )
            }
        }
        return buildList {
            if (project.doorWidthFt > 0 && project.doorHeightFt > 0) {
                val side = wallSide(project.doorWall)
                val wallLength = wallLength(project, side)
                add(
                    WallOpening(
                        type = OpeningType.DOOR,
                        wall = side,
                        offsetFt = ((wallLength - project.doorWidthFt) / 2.0).coerceAtLeast(0.0),
                        widthFt = project.doorWidthFt,
                        heightFt = project.doorHeightFt,
                        label = "Main door"
                    )
                )
            }
            if (project.windowCount > 0 && project.windowWidthFt > 0 && project.windowHeightFt > 0) {
                val side = wallSide(project.windowWall)
                val wallLength = wallLength(project, side)
                repeat(project.windowCount) { index ->
                    val spacing = wallLength / (project.windowCount + 1)
                    val center = spacing * (index + 1)
                    add(
                        WallOpening(
                            type = OpeningType.WINDOW,
                            wall = side,
                            offsetFt = (center - project.windowWidthFt / 2.0).coerceIn(0.0, (wallLength - project.windowWidthFt).coerceAtLeast(0.0)),
                            widthFt = project.windowWidthFt,
                            heightFt = project.windowHeightFt,
                            sillHeightFt = 3.0,
                            label = "Window ${index + 1}"
                        )
                    )
                }
            }
        }
    }

    fun wallLength(project: Project, wall: WallSide): Double = when (wall) {
        WallSide.FRONT, WallSide.REAR -> project.length
        WallSide.LEFT, WallSide.RIGHT -> project.width
    }

    fun framing(project: Project): FramingPlan {
        val openings = openings(project)
        val plans = WallSide.entries.map { wall ->
            WallFramingEngine.generate(
                wall = wall,
                wallLengthFt = wallLength(project, wall),
                wallHeightFt = project.height,
                studSpacingInches = project.studSpacingIn.toInt().coerceAtLeast(1),
                studSize = project.studSize,
                openings = openings,
                headerSize = null
            )
        }
        return FramingPlan(
            members = plans.flatMap { it.members },
            warnings = plans.flatMap { it.warnings }
        )
    }

    fun assistantSnapshot(project: Project): AssistantProjectSnapshot = AssistantProjectSnapshot(
        projectName = project.name,
        widthFt = project.width,
        lengthFt = project.length,
        heightFt = project.height,
        completedSteps = project.tasks.count { it.completed },
        totalSteps = project.tasks.size,
        estimatedCost = project.budget,
        actualCost = project.materials.filter { it.purchased }.sumOf { it.subtotal },
        unpurchasedMaterialCount = project.materials.count { !it.purchased },
        stalePriceCount = 0,
        framingWarnings = if (project.type == "Shed") framing(project).warnings.map { it.message } else emptyList(),
        codeWarnings = listOf("Jurisdiction-specific code rules have not been verified until an official source is selected."),
        nextStepTitle = project.tasks.firstOrNull { !it.completed }?.title
    )

    fun materialRequirements(project: Project): List<MaterialRequirement> = project.materials.map { material ->
        MaterialRequirement(
            key = material.id,
            description = material.name,
            quantityNeeded = material.quantity,
            unit = material.unit
        )
    }

    fun codeContext(project: Project): ProjectCodeContext = ProjectCodeContext(
        projectType = project.type,
        areaSqFt = project.area,
        heightFt = project.height
    )
}
