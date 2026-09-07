package com.buildright.assistant

import org.junit.Assert.*
import org.junit.Test

class IntentParserTest {
    @Test fun commonQuestionsClassify() {
        assertEquals(AssistantIntent.NEXT_STEP, IntentParser.classify("What should I do next?"))
        assertEquals(AssistantIntent.MISSING_MATERIALS, IntentParser.classify("What do I need to buy?"))
        assertEquals(AssistantIntent.WARNINGS, IntentParser.classify("Show anything that needs verification"))
    }
}
