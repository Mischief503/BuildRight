package com.buildright.assistant

data class AssistantProjectSnapshot(
    val projectName: String,
    val widthFt: Double,
    val lengthFt: Double,
    val heightFt: Double,
    val completedSteps: Int,
    val totalSteps: Int,
    val estimatedCost: Double,
    val actualCost: Double,
    val unpurchasedMaterialCount: Int,
    val stalePriceCount: Int,
    val framingWarnings: List<String>,
    val codeWarnings: List<String>,
    val nextStepTitle: String?
) {
    val progressPercent: Int
        get() = if (totalSteps <= 0) 0 else ((completedSteps.toDouble() / totalSteps) * 100).toInt()
}
