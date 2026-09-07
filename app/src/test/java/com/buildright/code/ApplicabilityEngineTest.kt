package com.buildright.code

import org.junit.Assert.*
import org.junit.Test

class ApplicabilityEngineTest {
    @Test fun projectTypeMismatchDoesNotApply() {
        val j = Jurisdiction("US","OR","Clackamas",null)
        val rule = CodeRule("r1",j,AuthorityLevel.COUNTY,RuleCategory.PERMIT,"Shed rule","",RuleStatus.CURRENT,
            CodeSource("source","county",null), listOf(RuleCondition.ProjectType("Shed")))
        val result = ApplicabilityEngine.evaluate(rule, ProjectCodeContext("Deck",100.0,8.0))
        assertFalse(result.applies)
    }
}
