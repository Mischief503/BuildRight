package com.buildright.code

data class CodeSnapshot(
    val jurisdiction: Jurisdiction,
    val rules: List<CodeRule>,
    val checkedAt: Long,
    val sourceRevision: String? = null
)

data class CodeChange(
    val ruleId: String,
    val oldTitle: String?,
    val newTitle: String?,
    val changeType: String
)

object CodeDiff {
    fun compare(old: CodeSnapshot, new: CodeSnapshot): List<CodeChange> {
        val oldMap = old.rules.associateBy { it.id }
        val newMap = new.rules.associateBy { it.id }
        val ids = oldMap.keys + newMap.keys
        return ids.mapNotNull { id ->
            val a = oldMap[id]
            val b = newMap[id]
            when {
                a == null && b != null -> CodeChange(id, null, b.title, "ADDED")
                a != null && b == null -> CodeChange(id, a.title, null, "REMOVED")
                a != null && b != null && a != b -> CodeChange(id, a.title, b.title, "CHANGED")
                else -> null
            }
        }
    }
}
