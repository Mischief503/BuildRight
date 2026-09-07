package com.buildright.app

import com.buildright.assistant.ProjectCommand
import java.util.UUID

/**
 * Natural-language project edit parser/executor used by the Smart Assistant.
 * It deliberately supports a small deterministic command set rather than
 * allowing generated text to mutate arbitrary project state.
 */
data class ProposedProjectEdit(
    val summary: String,
    val command: ProjectCommand
)

object ProjectCommandEngine {
    private val number = "([0-9]+(?:\\.[0-9]+)?)"

    fun propose(text: String, project: Project): ProposedProjectEdit? {
        val q = text.trim().lowercase()

        Regex("(?:set|change|make)\\s+(?:the\\s+)?width\\s+(?:to\\s+)?$number").find(q)?.let {
            val v = it.groupValues[1].toDouble()
            return ProposedProjectEdit("Change width from ${project.width} ft to $v ft", ProjectCommand.UpdateWidth(v))
        }
        Regex("(?:set|change|make)\\s+(?:the\\s+)?length\\s+(?:to\\s+)?$number").find(q)?.let {
            val v = it.groupValues[1].toDouble()
            return ProposedProjectEdit("Change length from ${project.length} ft to $v ft", ProjectCommand.UpdateLength(v))
        }
        Regex("(?:set|change|make)\\s+(?:the\\s+)?height\\s+(?:to\\s+)?$number").find(q)?.let {
            val v = it.groupValues[1].toDouble()
            return ProposedProjectEdit("Change wall height from ${project.height} ft to $v ft", ProjectCommand.UpdateHeight(v))
        }

        val addWindow = Regex("add\\s+(?:a\\s+)?window(?:\\s+(?:to|on)\\s+(front|rear|left|right))?").find(q)
        if (addWindow != null) {
            val wall = addWindow.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() } ?: project.windowWall
            return ProposedProjectEdit("Add a window on the $wall wall", ProjectCommand.AddWindow(wall, null))
        }
        val addDoor = Regex("add\\s+(?:a\\s+)?door(?:\\s+(?:to|on)\\s+(front|rear|left|right))?").find(q)
        if (addDoor != null) {
            val wall = addDoor.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() } ?: project.doorWall
            return ProposedProjectEdit("Add a door on the $wall wall", ProjectCommand.AddDoor(wall, null))
        }

        if (q.contains("remove") && q.contains("window")) {
            val opening = project.openings.firstOrNull { it.type.equals("WINDOW", true) } ?: return null
            return ProposedProjectEdit("Remove ${opening.label.ifBlank { "window" }}", ProjectCommand.RemoveOpening(opening.id))
        }
        if (q.contains("remove") && q.contains("door")) {
            val opening = project.openings.firstOrNull { it.type.equals("DOOR", true) } ?: return null
            return ProposedProjectEdit("Remove ${opening.label.ifBlank { "door" }}", ProjectCommand.RemoveOpening(opening.id))
        }

        val move = Regex("move\\s+(?:the\\s+)?(?:window|door|opening).*?$number\\s*(?:ft|feet|foot)?\\s*(left|right)").find(q)
        if (move != null) {
            val amount = move.groupValues[1].toDouble()
            val direction = move.groupValues[2]
            val type = if (q.contains("door")) "DOOR" else "WINDOW"
            val opening = project.openings.firstOrNull { it.type.equals(type, true) } ?: return null
            val wallLength = if (opening.wall == "Front" || opening.wall == "Rear") project.length else project.width
            val delta = if (direction == "left") -amount else amount
            val newOffset = (opening.offsetFt + delta).coerceIn(0.0, (wallLength - opening.widthFt).coerceAtLeast(0.0))
            return ProposedProjectEdit("Move ${opening.label.ifBlank { type.lowercase() }} $amount ft $direction", ProjectCommand.MoveOpening(opening.id, newOffset))
        }
        return null
    }

    fun execute(command: ProjectCommand, project: Project): String {
        return when (command) {
            is ProjectCommand.UpdateWidth -> { project.width = command.feet; "Width updated to ${command.feet} ft." }
            is ProjectCommand.UpdateLength -> { project.length = command.feet; "Length updated to ${command.feet} ft." }
            is ProjectCommand.UpdateHeight -> { project.height = command.feet; "Wall height updated to ${command.feet} ft." }
            is ProjectCommand.AddWindow -> {
                val wallLength = if (command.wall == "Front" || command.wall == "Rear") project.length else project.width
                val width = project.windowWidthFt.coerceAtLeast(1.0)
                project.openings += ProjectOpening(
                    type = "WINDOW", wall = command.wall,
                    offsetFt = command.offsetFt ?: ((wallLength - width) / 2.0).coerceAtLeast(0.0),
                    widthFt = width, heightFt = project.windowHeightFt.coerceAtLeast(1.0),
                    sillHeightFt = 3.0, label = "Window ${project.openings.count { it.type == "WINDOW" } + 1}"
                )
                syncLegacyOpenings(project)
                "Window added on the ${command.wall} wall."
            }
            is ProjectCommand.AddDoor -> {
                val wallLength = if (command.wall == "Front" || command.wall == "Rear") project.length else project.width
                val width = project.doorWidthFt.coerceAtLeast(1.0)
                project.openings += ProjectOpening(
                    type = "DOOR", wall = command.wall,
                    offsetFt = command.offsetFt ?: ((wallLength - width) / 2.0).coerceAtLeast(0.0),
                    widthFt = width, heightFt = project.doorHeightFt.coerceAtLeast(1.0),
                    sillHeightFt = 0.0, label = "Door"
                )
                syncLegacyOpenings(project)
                "Door added on the ${command.wall} wall."
            }
            is ProjectCommand.MoveOpening -> {
                val x = project.openings.firstOrNull { it.id == command.openingId }
                if (x == null) "Opening was not found." else { x.offsetFt = command.newOffsetFt; syncLegacyOpenings(project); "${x.label.ifBlank { "Opening" }} moved." }
            }
            is ProjectCommand.RemoveOpening -> {
                val removed = project.openings.firstOrNull { it.id == command.openingId }
                if (removed == null) "Opening was not found." else { project.openings.remove(removed); syncLegacyOpenings(project); "${removed.label.ifBlank { "Opening" }} removed." }
            }
            is ProjectCommand.Navigate -> "Navigation requested: ${command.destination}"
        }
    }

    fun syncLegacyOpenings(project: Project) {
        val doors = project.openings.filter { it.type.equals("DOOR", true) }
        val windows = project.openings.filter { it.type.equals("WINDOW", true) }
        doors.firstOrNull()?.let {
            project.doorWall = it.wall; project.doorWidthFt = it.widthFt; project.doorHeightFt = it.heightFt
        }
        project.windowCount = windows.size
        windows.firstOrNull()?.let {
            project.windowWall = it.wall; project.windowWidthFt = it.widthFt; project.windowHeightFt = it.heightFt
        }
    }
}
