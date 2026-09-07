package com.buildright.assistant

data class ProjectMetricChange(
    val metric: String,
    val before: Double,
    val after: Double,
    val unit: String,
    val causes: List<String>
)

object ChangeReasonFormatter {
    fun explain(change: ProjectMetricChange): String {
        val delta = change.after - change.before
        val direction = if (delta >= 0) "increased" else "decreased"
        val causes = if (change.causes.isEmpty()) "No recorded cause was found."
        else change.causes.joinToString("; ")
        return "${change.metric} $direction from ${change.before} to ${change.after} ${change.unit}. $causes"
    }
}
