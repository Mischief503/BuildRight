package com.buildright.assistant

enum class ActionRisk { READ_ONLY, PROJECT_EDIT, STRUCTURAL, CODE }

data class ActionDecision(
    val allowed: Boolean,
    val risk: ActionRisk,
    val reason: String
)

object AssistantActionPolicy {
    fun classify(command: ProjectCommand?): ActionDecision {
        if (command == null) return ActionDecision(true, ActionRisk.READ_ONLY, "Read-only response.")
        return when (command) {
            is ProjectCommand.Navigate ->
                ActionDecision(true, ActionRisk.READ_ONLY, "Navigation only.")
            is ProjectCommand.MoveOpening,
            is ProjectCommand.RemoveOpening,
            is ProjectCommand.AddDoor,
            is ProjectCommand.AddWindow,
            is ProjectCommand.UpdateWidth,
            is ProjectCommand.UpdateLength,
            is ProjectCommand.UpdateHeight ->
                ActionDecision(true, ActionRisk.PROJECT_EDIT, "Project edit requires validation and user confirmation.")
        }
    }

    fun structuralRequest(reason: String) =
        ActionDecision(false, ActionRisk.STRUCTURAL,
            "The assistant cannot invent structural design values. $reason")

    fun codeRequest(reason: String) =
        ActionDecision(false, ActionRisk.CODE,
            "The assistant must use a verified jurisdiction/source record for code guidance. $reason")
}
