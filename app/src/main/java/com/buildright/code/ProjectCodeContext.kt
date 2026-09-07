package com.buildright.code

data class ProjectCodeContext(
    val projectType: String,
    val areaSqFt: Double,
    val heightFt: Double,
    val hasElectrical: Boolean = false,
    val hasPlumbing: Boolean = false,
    val hasHabitableUse: Boolean = false,
    val distanceToPropertyLinesFt: Map<String, Double> = emptyMap()
)

data class RuleApplicability(
    val rule: CodeRule,
    val applies: Boolean,
    val reason: String
)

data class PermitRequirement(
    val name: String,
    val state: RequirementState,
    val reason: String,
    val sourceRuleId: String? = null
)
