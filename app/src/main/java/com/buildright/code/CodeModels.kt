package com.buildright.code

enum class AuthorityLevel { COUNTRY, STATE, COUNTY, CITY, DISTRICT }
enum class RuleCategory {
    PERMIT, ZONING, SETBACK, HEIGHT, AREA, FOUNDATION,
    FRAMING, ROOFING, ELECTRICAL, PLUMBING, FIRE,
    ENERGY, ACCESSIBILITY, OTHER
}
enum class RuleStatus { CURRENT, SUPERSEDED, DRAFT, UNKNOWN }
enum class RequirementState { UNKNOWN, REQUIRED, NOT_REQUIRED, CONDITIONAL }

data class Jurisdiction(
    val country: String,
    val stateOrProvince: String? = null,
    val county: String? = null,
    val city: String? = null,
    val district: String? = null
) {
    fun displayName(): String =
        listOfNotNull(city, county, stateOrProvince, country).joinToString(", ")
}

data class CodeSource(
    val title: String,
    val issuingAuthority: String,
    val sourceUrl: String?,
    val documentId: String? = null,
    val publishedAt: Long? = null,
    val effectiveFrom: Long? = null,
    val effectiveTo: Long? = null
)

data class CodeRule(
    val id: String,
    val jurisdiction: Jurisdiction,
    val authorityLevel: AuthorityLevel,
    val category: RuleCategory,
    val title: String,
    val summary: String,
    val status: RuleStatus,
    val source: CodeSource,
    val conditions: List<RuleCondition> = emptyList(),
    val notes: String = ""
)

sealed class RuleCondition {
    data class MaxAreaSqFt(val value: Double) : RuleCondition()
    data class MaxHeightFt(val value: Double) : RuleCondition()
    data class MinSetbackFt(val value: Double, val boundary: String) : RuleCondition()
    data class ProjectType(val type: String) : RuleCondition()
    data class HasUtilities(val value: Boolean) : RuleCondition()
    data class Custom(val key: String, val operator: String, val value: String) : RuleCondition()
}
