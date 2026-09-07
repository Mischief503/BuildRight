package com.buildright.assistant

object ProjectExplainer {
    fun summary(s: AssistantProjectSnapshot): String = buildString {
        append("${s.projectName}: ${s.widthFt} x ${s.lengthFt} ft, ")
        append("${s.progressPercent}% complete. ")
        append("Estimated cost $${"%.2f".format(s.estimatedCost)}")
        if (s.actualCost > 0) append(", actual cost $${"%.2f".format(s.actualCost)}")
        append(". ")
        if (s.unpurchasedMaterialCount > 0) {
            append("${s.unpurchasedMaterialCount} material item(s) still need purchasing. ")
        }
        if (s.stalePriceCount > 0) {
            append("${s.stalePriceCount} cached price(s) are stale. ")
        }
        if (s.nextStepTitle != null) append("Next step: ${s.nextStepTitle}.")
    }

    fun warnings(s: AssistantProjectSnapshot): List<String> =
        s.framingWarnings + s.codeWarnings
}
