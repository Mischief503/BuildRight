package com.buildright.code

object ApplicabilityEngine {

    fun evaluate(rule: CodeRule, project: ProjectCodeContext): RuleApplicability {
        for (condition in rule.conditions) {
            when (condition) {
                is RuleCondition.MaxAreaSqFt ->
                    if (project.areaSqFt > condition.value)
                        return RuleApplicability(rule, false, "Project exceeds ${condition.value} sq ft condition.")
                is RuleCondition.MaxHeightFt ->
                    if (project.heightFt > condition.value)
                        return RuleApplicability(rule, false, "Project exceeds ${condition.value} ft condition.")
                is RuleCondition.ProjectType ->
                    if (!project.projectType.equals(condition.type, ignoreCase = true))
                        return RuleApplicability(rule, false, "Rule applies to ${condition.type} projects.")
                is RuleCondition.HasUtilities ->
                    if (condition.value && !(project.hasElectrical || project.hasPlumbing))
                        return RuleApplicability(rule, false, "Rule applies when utilities are present.")
                is RuleCondition.MinSetbackFt -> {
                    val actual = project.distanceToPropertyLinesFt[condition.boundary]
                    if (actual != null && actual < condition.value)
                        return RuleApplicability(rule, true, "Setback may be deficient: $actual ft vs ${condition.value} ft.")
                }
                is RuleCondition.Custom -> Unit
            }
        }
        return RuleApplicability(rule, true, "Rule matches the current project context.")
    }

    fun applicable(rules: List<CodeRule>, project: ProjectCodeContext): List<RuleApplicability> =
        rules.map { evaluate(it, project) }.filter { it.applies }
}
