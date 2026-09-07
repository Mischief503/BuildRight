package com.buildright.assistant

enum class AssistantIntent {
    PROJECT_SUMMARY,
    NEXT_STEP,
    MISSING_MATERIALS,
    COST_STATUS,
    PRICE_FRESHNESS,
    EXPLAIN_CHANGE,
    WARNINGS,
    MOVE_OPENING,
    ADD_OPENING,
    REMOVE_OPENING,
    UPDATE_DIMENSION,
    NAVIGATE,
    UNKNOWN
}

data class AssistantRequest(
    val text: String,
    val projectId: String
)

data class AssistantReply(
    val message: String,
    val command: ProjectCommand? = null,
    val requiresConfirmation: Boolean = false,
    val warnings: List<String> = emptyList()
)

sealed class ProjectCommand {
    data class MoveOpening(val openingId: String, val newOffsetFt: Double) : ProjectCommand()
    data class RemoveOpening(val openingId: String) : ProjectCommand()
    data class AddDoor(val wall: String, val offsetFt: Double?) : ProjectCommand()
    data class AddWindow(val wall: String, val offsetFt: Double?) : ProjectCommand()
    data class UpdateWidth(val feet: Double) : ProjectCommand()
    data class UpdateLength(val feet: Double) : ProjectCommand()
    data class UpdateHeight(val feet: Double) : ProjectCommand()
    data class Navigate(val destination: String) : ProjectCommand()
}
