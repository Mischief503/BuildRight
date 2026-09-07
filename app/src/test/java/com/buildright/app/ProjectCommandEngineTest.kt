package com.buildright.app

import org.junit.Assert.*
import org.junit.Test

class ProjectCommandEngineTest {
    @Test fun proposesAndExecutesWidthEdit() {
        val p=Project(width=10.0,length=12.0,height=8.0)
        val proposal=ProjectCommandEngine.propose("set width to 14",p)
        assertNotNull(proposal)
        ProjectCommandEngine.execute(proposal!!.command,p)
        assertEquals(14.0,p.width,0.001)
    }

    @Test fun addsPersistableOpening() {
        val p=Project(width=10.0,length=12.0,height=8.0)
        val proposal=ProjectCommandEngine.propose("add a window on left",p)
        assertNotNull(proposal)
        ProjectCommandEngine.execute(proposal!!.command,p)
        assertEquals(1,p.openings.size)
        assertEquals("Left",p.openings.first().wall)
    }
}
