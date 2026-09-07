package com.buildright.assistant

object CommandValidator {
    fun validate(command: ProjectCommand): List<String> {
        val errors = mutableListOf<String>()
        when (command) {
            is ProjectCommand.MoveOpening ->
                if (command.newOffsetFt < 0) errors += "Opening offset cannot be negative."
            is ProjectCommand.UpdateWidth ->
                if (command.feet <= 0) errors += "Width must be greater than zero."
            is ProjectCommand.UpdateLength ->
                if (command.feet <= 0) errors += "Length must be greater than zero."
            is ProjectCommand.UpdateHeight ->
                if (command.feet <= 0) errors += "Height must be greater than zero."
            is ProjectCommand.AddDoor ->
                if (command.offsetFt != null && command.offsetFt < 0) errors += "Door offset cannot be negative."
            is ProjectCommand.AddWindow ->
                if (command.offsetFt != null && command.offsetFt < 0) errors += "Window offset cannot be negative."
            is ProjectCommand.RemoveOpening,
            is ProjectCommand.Navigate -> Unit
        }
        return errors
    }

    fun requiresConfirmation(command: ProjectCommand): Boolean =
        command !is ProjectCommand.Navigate
}
