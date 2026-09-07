package com.buildright.ui

/**
 * UI contract for the Compose Field Mode screen.
 *
 * The production screen should keep the active step visible and use large controls:
 * - Previous / Complete & Next
 * - Add measurement
 * - Add photo
 * - Add receipt
 * - Add field note
 * - Show plan for this stage
 *
 * Dense project configuration stays in Design Mode.
 */
data class FieldModeState(
    val projectName: String,
    val stageName: String,
    val stepTitle: String,
    val stepInstructions: String,
    val completedSteps: Int,
    val totalSteps: Int,
    val measurementsThisStage: Int,
    val attachmentsThisStage: Int
) {
    val progress: Float
        get() = if (totalSteps == 0) 0f else completedSteps.toFloat() / totalSteps
}
