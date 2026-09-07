package com.buildright.assistant

object IntentParser {
    fun classify(text: String): AssistantIntent {
        val t = text.lowercase()
        return when {
            "what do i still need" in t || "what do i need to buy" in t || "missing material" in t ->
                AssistantIntent.MISSING_MATERIALS
            "next step" in t || "what should i do next" in t ->
                AssistantIntent.NEXT_STEP
            "price" in t && ("old" in t || "stale" in t || "refresh" in t) ->
                AssistantIntent.PRICE_FRESHNESS
            "cost" in t || "budget" in t || "how much" in t ->
                AssistantIntent.COST_STATUS
            "warning" in t || "needs verification" in t || "verify" in t ->
                AssistantIntent.WARNINGS
            "why" in t && ("change" in t || "changed" in t || "increase" in t || "decrease" in t) ->
                AssistantIntent.EXPLAIN_CHANGE
            "move" in t && ("window" in t || "door" in t || "opening" in t) ->
                AssistantIntent.MOVE_OPENING
            "add" in t && "door" in t -> AssistantIntent.ADD_OPENING
            "add" in t && "window" in t -> AssistantIntent.ADD_OPENING
            "remove" in t && ("window" in t || "door" in t || "opening" in t) ->
                AssistantIntent.REMOVE_OPENING
            "width" in t || "length" in t || "height" in t ->
                AssistantIntent.UPDATE_DIMENSION
            "show me" in t || "open" in t || "go to" in t ->
                AssistantIntent.NAVIGATE
            "summary" in t || "where are we" in t || "project status" in t ->
                AssistantIntent.PROJECT_SUMMARY
            else -> AssistantIntent.UNKNOWN
        }
    }
}
