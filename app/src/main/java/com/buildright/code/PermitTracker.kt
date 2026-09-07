package com.buildright.code

enum class PermitStage {
    NOT_STARTED, RESEARCHING, APPLICATION_READY, SUBMITTED,
    REVISION_REQUESTED, APPROVED, INSPECTION_REQUIRED, CLOSED
}

data class PermitRecord(
    val id: String,
    val permitType: String,
    val jurisdiction: Jurisdiction,
    val stage: PermitStage,
    val applicationNumber: String? = null,
    val submittedAt: Long? = null,
    val approvedAt: Long? = null,
    val notes: String = "",
    val documentUris: List<String> = emptyList()
)
